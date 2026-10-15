package com.whatsapp.clone.network

import android.util.Log
import com.whatsapp.clone.model.Message
import com.whatsapp.clone.model.MessageStatus
import com.whatsapp.clone.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class IncomingMessageEvent(
    val messageId: String,
    val senderJid: String,
    val senderName: String,
    val text: String,
    val type: MessageType,
    val timestampMs: Long
)

data class IncomingReceiptEvent(
    val messageId: String,
    val status: MessageStatus
)

class WebSocketClientManager(
    private val scope: CoroutineScope,
    private val onIncomingMessage: (IncomingMessageEvent) -> Unit,
    private val onIncomingReceipt: (IncomingReceiptEvent) -> Unit
) {
    companion object {
        private const val TAG = "WebSocketClientManager"
        // 10.0.2.2 maps to the development host machine in the standard Android Emulator
        const val DEFAULT_EMULATOR_URL = "ws://10.0.2.2:8080"
        const val DEFAULT_LOCAL_URL = "ws://127.0.0.1:8080"
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var myJid: String = "client_me@s.whatsapp.net"

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun connect(serverUrl: String = DEFAULT_EMULATOR_URL, jid: String = "client_me@s.whatsapp.net") {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED || _connectionStatus.value == ConnectionStatus.CONNECTING) {
            return
        }

        this.myJid = jid
        _connectionStatus.value = ConnectionStatus.CONNECTING
        Log.i(TAG, "Connecting to WebSocket server at $serverUrl as $jid ...")

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully! Sending auth handshake...")
                _connectionStatus.value = ConnectionStatus.CONNECTED

                // Send Auth Handshake
                val authPayload = JSONObject().apply {
                    put("action", "auth")
                    put("jid", myJid)
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(authPayload.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingJson(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code / $reason")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message} (Will allow offline mode/local persistence)")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        })
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Client closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket", e)
        }
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun sendMessageStanza(message: Message, recipientJid: String = "engineering@s.whatsapp.net") {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED || webSocket == null) {
            Log.i(TAG, "WebSocket not connected. Message stored in local SQLite msgstore.db (offline queue).")
            return
        }

        try {
            val stanza = JSONObject().apply {
                put("message_id", message.id)
                put("sender_jid", myJid)
                put("recipient_jid", recipientJid)
                put("timestamp_ms", System.currentTimeMillis())
                put("type", message.type.name)
                put("status", message.status.name)
                put("text_content", message.text)
                put("file_name", message.fileName)
                put("file_size", message.fileSize)
                put("audio_duration_seconds", message.audioDurationSeconds)
            }

            val payload = JSONObject().apply {
                put("action", "message")
                put("stanza", stanza)
            }

            webSocket?.send(payload.toString())
            Log.i(TAG, "Message stanza ${message.id} sent over WebSocket wire.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message over WebSocket", e)
        }
    }

    fun sendReadReceipt(messageId: String, senderJid: String) {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        try {
            val receipt = JSONObject().apply {
                put("message_id", messageId)
                put("recipient_jid", senderJid)
                put("status", "READ")
                put("timestamp", System.currentTimeMillis())
            }
            val payload = JSONObject().apply {
                put("action", "receipt")
                put("receipt", receipt)
            }
            webSocket?.send(payload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send read receipt", e)
        }
    }

    private fun handleIncomingJson(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            when (root.optString("action")) {
                "message" -> {
                    val stanza = root.optJSONObject("stanza") ?: return
                    val msgId = stanza.optString("message_id")
                    val senderJid = stanza.optString("sender_jid")
                    val senderName = stanza.optString("sender_name", "Contato")
                    val text = stanza.optString("text_content")
                    val typeStr = stanza.optString("type", "TEXT")
                    val type = try { MessageType.valueOf(typeStr) } catch (e: Exception) { MessageType.TEXT }
                    val ts = stanza.optLong("timestamp_ms", System.currentTimeMillis())

                    scope.launch(Dispatchers.Main) {
                        onIncomingMessage(
                            IncomingMessageEvent(
                                messageId = msgId,
                                senderJid = senderJid,
                                senderName = senderName,
                                text = text,
                                type = type,
                                timestampMs = ts
                            )
                        )
                    }

                    // Acknowledge read receipt back
                    sendReadReceipt(msgId, senderJid)
                }

                "receipt" -> {
                    val receipt = root.optJSONObject("receipt") ?: return
                    val msgId = receipt.optString("message_id")
                    val statusStr = receipt.optString("status")
                    val status = when (statusStr) {
                        "SENT" -> MessageStatus.SENT
                        "DELIVERED" -> MessageStatus.DELIVERED
                        "READ" -> MessageStatus.READ
                        else -> MessageStatus.SENT
                    }

                    scope.launch(Dispatchers.Main) {
                        onIncomingReceipt(
                            IncomingReceiptEvent(
                                messageId = msgId,
                                status = status
                            )
                        )
                    }
                }

                "auth_ack" -> {
                    Log.i(TAG, "Server auth confirmed: ${root.optString("server_version")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming json message", e)
        }
    }
}
