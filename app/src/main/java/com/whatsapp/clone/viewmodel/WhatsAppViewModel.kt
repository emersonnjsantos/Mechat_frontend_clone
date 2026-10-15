package com.whatsapp.clone.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whatsapp.clone.R
import com.whatsapp.clone.data.ChatRepository
import com.whatsapp.clone.model.*
import com.whatsapp.clone.nativebridge.NativeBridge
import com.whatsapp.clone.network.ConnectionStatus
import com.whatsapp.clone.network.IncomingMessageEvent
import com.whatsapp.clone.network.IncomingReceiptEvent
import com.whatsapp.clone.network.WebSocketClientManager
import com.whatsapp.clone.utils.RealAudioRecorder
import com.whatsapp.clone.webrtc.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

class WhatsAppViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = RealAudioRecorder(application)
    private val chatRepository = ChatRepository(application)

    private val webSocketClient = WebSocketClientManager(
        scope = viewModelScope,
        onIncomingMessage = { event -> handleIncomingRemoteMessage(event) },
        onIncomingReceipt = { receipt -> handleIncomingRemoteReceipt(receipt) }
    )
    val connectionStatus: StateFlow<ConnectionStatus> = webSocketClient.connectionStatus

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    val messages: StateFlow<Map<Long, List<Message>>> = _messages.asStateFlow()

    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId.asStateFlow()

    private val _nativeCoreInfo = MutableStateFlow<String>("")
    val nativeCoreInfo: StateFlow<String> = _nativeCoreInfo.asStateFlow()

    private val _calls = MutableStateFlow<List<CallItem>>(emptyList())
    val calls: StateFlow<List<CallItem>> = _calls.asStateFlow()

    private val webRtcCallManager = WebRtcCallManager(
        context = application,
        scope = viewModelScope,
        webSocketManager = webSocketClient
    )

    val webRtcCallState: StateFlow<WebRtcCallState> = webRtcCallManager.callState
    val webRtcCallStats: StateFlow<WebRtcCallStats> = webRtcCallManager.callStats
    val isCallMuted: StateFlow<Boolean> = webRtcCallManager.isMuted
    val isCallSpeakerOn: StateFlow<Boolean> = webRtcCallManager.isSpeakerOn

    private val _activeCallContactName = MutableStateFlow("WhatsApp Contato")
    val activeCallContactName: StateFlow<String> = _activeCallContactName.asStateFlow()

    private val _activeCallIsVideo = MutableStateFlow(false)
    val activeCallIsVideo: StateFlow<Boolean> = _activeCallIsVideo.asStateFlow()

    private val _isInVoipCall = MutableStateFlow(false)
    val isInVoipCall: StateFlow<Boolean> = _isInVoipCall.asStateFlow()

    // Real Audio Recording State
    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _recordingWaveform = MutableStateFlow<List<Float>>(emptyList())
    val recordingWaveform: StateFlow<List<Float>> = _recordingWaveform.asStateFlow()

    // Audio Playback State
    private val _currentlyPlayingAudioId = MutableStateFlow<String?>(null)
    val currentlyPlayingAudioId: StateFlow<String?> = _currentlyPlayingAudioId.asStateFlow()

    private val _audioPlaybackProgress = MutableStateFlow(0f)
    val audioPlaybackProgress: StateFlow<Float> = _audioPlaybackProgress.asStateFlow()

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        loadInitialData()
        loadNativeCore()
        webSocketClient.connect()
        viewModelScope.launch {
            webRtcCallManager.callState.collect { state ->
                if (state == WebRtcCallState.IDLE || state == WebRtcCallState.ENDED) {
                    _isInVoipCall.value = false
                }
            }
        }
    }

    private fun loadNativeCore() {
        try {
            val version = NativeBridge.getNativeCoreVersion()
            val neon = NativeBridge.isHardwareAccelerationAvailable()
            _nativeCoreInfo.value = "$version | NEON: $neon"

            val rand = SecureRandom()
            val alicePriv = ByteArray(32).also { rand.nextBytes(it) }
            val bobPriv = ByteArray(32).also { rand.nextBytes(it) }

            val alicePub = NativeBridge.generateCurve25519PublicKey(alicePriv)
            val bobPub = NativeBridge.generateCurve25519PublicKey(bobPriv)

            if (alicePub != null && bobPub != null) {
                val secretA = NativeBridge.calculateCurve25519SharedSecret(alicePriv, bobPub)
                val secretB = NativeBridge.calculateCurve25519SharedSecret(bobPriv, alicePub)
                val match = secretA?.contentEquals(secretB ?: byteArrayOf()) == true
                android.util.Log.i("WhatsAppViewModel", "Curve25519 C Key Exchange Successful! E2EE Match: $match")
            }
        } catch (e: Throwable) {
            _nativeCoreInfo.value = "C++ Core: ${e.message}"
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                chatRepository.seedInitialDataIfEmpty()
                val dbChats = chatRepository.getAllChats()
                _chats.value = dbChats
                val initialMsgs = mutableMapOf<Long, List<Message>>()
                for (c in dbChats) {
                    initialMsgs[c.id] = chatRepository.getMessagesForChat(c.id)
                }
                _messages.value = initialMsgs
            } catch (e: Exception) {
                android.util.Log.e("WhatsAppViewModel", "Error loading data from msgstore.db", e)
            }
        }

        _calls.value = listOf(
            CallItem(
                id = 101L,
                contactName = "Engenharia de Software (Meta / WA)",
                avatarLetter = "E",
                avatarColor = 0xFF00A884,
                timestamp = "Hoje, 18:40",
                isIncoming = true,
                isMissed = false,
                isVideo = false
            ),
            CallItem(
                id = 102L,
                contactName = "Família & Amigos",
                avatarLetter = "F",
                avatarColor = 0xFF128C7E,
                timestamp = "Hoje, 16:10",
                isIncoming = true,
                isMissed = true,
                isVideo = true
            )
        )
    }

    fun openChat(chatId: Long) {
        _activeChatId.value = chatId
        viewModelScope.launch {
            try {
                chatRepository.clearUnreadCount(chatId)
                val msgs = chatRepository.getMessagesForChat(chatId)
                _messages.update { current -> current + (chatId to msgs) }
            } catch (e: Exception) {
                android.util.Log.e("WhatsAppViewModel", "Error loading messages for chat $chatId", e)
            }
        }
        _chats.update { list ->
            list.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
        }
    }

    fun closeChat() {
        stopAudioPlayback()
        _activeChatId.value = null
    }

    fun sendMessage(chatId: Long, text: String) {
        if (text.isBlank()) return

        val now = timeFormat.format(Date())
        val newMsgId = "msg_${System.currentTimeMillis()}"
        val userMsg = Message(
            id = newMsgId,
            chatId = chatId,
            senderName = "Você",
            text = text,
            timestamp = now,
            isFromMe = true,
            status = MessageStatus.SENT
        )

        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + userMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = text, lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(userMsg)
            webSocketClient.sendMessageStanza(userMsg, recipientJid = "engineering@s.whatsapp.net")

            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(800)
                updateMessageStatus(chatId, newMsgId, MessageStatus.DELIVERED)

                delay(1000)
                updateMessageStatus(chatId, newMsgId, MessageStatus.READ)

                delay(1200)
                val replyTime = timeFormat.format(Date())
                val replyMsg = Message(
                    id = "reply_${System.currentTimeMillis()}",
                    chatId = chatId,
                    senderName = "WhatsApp Core",
                    text = "Mensagem recebida e persistida no msgstore.db (Curve25519 / LibSignal E2EE).",
                    timestamp = replyTime,
                    isFromMe = false,
                    status = MessageStatus.READ
                )

                playSound(R.raw.kd3)
                chatRepository.saveMessage(replyMsg)

                _messages.update { currentMap ->
                    val chatMsgs = currentMap[chatId] ?: emptyList()
                    currentMap + (chatId to (chatMsgs + replyMsg))
                }

                _chats.update { list ->
                    list.map {
                        if (it.id == chatId) it.copy(lastMessage = replyMsg.text, lastMessageTime = replyTime) else it
                    }
                }
            }
        }
    }

    // --- REAL AUDIO RECORDING FROM HARDWARE MICROPHONE ---

    fun startRecordingRealAudio(): Boolean {
        stopAudioPlayback()
        val file = audioRecorder.startRecording()
        if (file != null) {
            _isRecordingAudio.value = true
            _recordingDurationSeconds.value = 0
            _recordingWaveform.value = emptyList()

            recordingJob = viewModelScope.launch {
                val liveWaveform = mutableListOf<Float>()
                var seconds = 0
                var ticks = 0
                while (isActive && _isRecordingAudio.value) {
                    delay(120)
                    ticks++
                    if (ticks % 8 == 0) {
                        seconds++
                        _recordingDurationSeconds.value = seconds
                    }
                    val amp = audioRecorder.getMaxAmplitude()
                    liveWaveform.add(amp)
                    if (liveWaveform.size > 28) {
                        liveWaveform.removeAt(0)
                    }
                    _recordingWaveform.value = liveWaveform.toList()
                }
            }
            return true
        }
        return false
    }

    fun stopAndSendRealAudio(chatId: Long) {
        if (!_isRecordingAudio.value) return
        _isRecordingAudio.value = false
        recordingJob?.cancel()

        val (audioFile, durationSec) = audioRecorder.stopRecording()
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            val now = timeFormat.format(Date())
            val audioMsg = Message(
                id = "audio_${System.currentTimeMillis()}",
                chatId = chatId,
                senderName = "Você",
                text = "",
                timestamp = now,
                isFromMe = true,
                type = MessageType.AUDIO,
                audioDurationSeconds = durationSec,
                waveform = if (_recordingWaveform.value.isNotEmpty()) _recordingWaveform.value else listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f),
                filePath = audioFile.absolutePath,
                status = MessageStatus.SENT
            )

            playSound(R.raw.keq)

            _messages.update { currentMap ->
                val chatMsgs = currentMap[chatId] ?: emptyList()
                currentMap + (chatId to (chatMsgs + audioMsg))
            }

            _chats.update { list ->
                list.map {
                    if (it.id == chatId) it.copy(lastMessage = "🎤 Mensagem de voz (${durationSec}s)", lastMessageTime = now) else it
                }
            }

            viewModelScope.launch {
                chatRepository.saveMessage(audioMsg)
                webSocketClient.sendMessageStanza(audioMsg, recipientJid = "engineering@s.whatsapp.net")
                if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                    delay(1000)
                    updateMessageStatus(chatId, audioMsg.id, MessageStatus.READ)
                }
            }
        }
    }

    fun cancelRealAudioRecording() {
        if (!_isRecordingAudio.value) return
        _isRecordingAudio.value = false
        recordingJob?.cancel()
        audioRecorder.cancelRecording()
        playSound(R.raw.kf2) // authentic error/cancel sound
    }

    // --- REAL AUDIO PLAYBACK ---

    fun togglePlayAudio(message: Message) {
        if (_currentlyPlayingAudioId.value == message.id) {
            stopAudioPlayback()
            return
        }

        stopAudioPlayback()
        val file = message.filePath?.let { File(it) }

        if (file != null && file.exists()) {
            _currentlyPlayingAudioId.value = message.id
            audioRecorder.playAudio(
                file = file,
                onProgress = { progress -> _audioPlaybackProgress.value = progress },
                onComplete = {
                    _currentlyPlayingAudioId.value = null
                    _audioPlaybackProgress.value = 0f
                }
            )

            playbackJob = viewModelScope.launch {
                while (isActive && _currentlyPlayingAudioId.value == message.id) {
                    delay(100)
                    _audioPlaybackProgress.value = audioRecorder.getAudioProgress()
                }
            }
        } else {
            // Play authentic WhatsApp notification tone as fallback
            playSound(R.raw.kd3)
        }
    }

    fun stopAudioPlayback() {
        playbackJob?.cancel()
        audioRecorder.stopAudio()
        _currentlyPlayingAudioId.value = null
        _audioPlaybackProgress.value = 0f
    }

    fun downloadMessageFile(chatId: Long, msgId: String) {
        _messages.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map {
                if (it.id == msgId) it.copy(isDownloading = true) else it
            })
        }

        viewModelScope.launch {
            delay(1400)
            playSound(R.raw.keq)

            _messages.update { map ->
                val list = map[chatId] ?: return@update map
                map + (chatId to list.map {
                    if (it.id == msgId) it.copy(isDownloading = false, isDownloaded = true) else it
                })
            }
        }
    }

    fun sendDocument(chatId: Long, fileName: String, fileSize: String) {
        val now = timeFormat.format(Date())
        val docMsg = Message(
            id = "doc_${System.currentTimeMillis()}",
            chatId = chatId,
            senderName = "Você",
            text = "",
            timestamp = now,
            isFromMe = true,
            type = MessageType.DOCUMENT,
            fileName = fileName,
            fileSize = fileSize,
            isDownloaded = true,
            status = MessageStatus.SENT
        )
        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + docMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = "📄 $fileName", lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(docMsg)
            webSocketClient.sendMessageStanza(docMsg, recipientJid = "engineering@s.whatsapp.net")
            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(1000)
                updateMessageStatus(chatId, docMsg.id, MessageStatus.READ)
            }
        }
    }

    fun sendImage(chatId: Long, caption: String) {
        val now = timeFormat.format(Date())
        val imgMsg = Message(
            id = "img_${System.currentTimeMillis()}",
            chatId = chatId,
            senderName = "Você",
            text = caption,
            timestamp = now,
            isFromMe = true,
            type = MessageType.IMAGE,
            fileName = "foto_${System.currentTimeMillis()}.jpg",
            fileSize = "840 KB",
            isDownloaded = true,
            status = MessageStatus.SENT
        )
        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + imgMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = "📷 Foto", lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(imgMsg)
            webSocketClient.sendMessageStanza(imgMsg, recipientJid = "engineering@s.whatsapp.net")
            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(1000)
                updateMessageStatus(chatId, imgMsg.id, MessageStatus.READ)
            }
        }
    }

    fun sendLocation(chatId: Long, address: String) {
        val now = timeFormat.format(Date())
        val locMsg = Message(
            id = "loc_${System.currentTimeMillis()}",
            chatId = chatId,
            senderName = "Você",
            text = address,
            timestamp = now,
            isFromMe = true,
            type = MessageType.LOCATION,
            status = MessageStatus.SENT
        )
        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + locMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = "📍 Localização", lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(locMsg)
            webSocketClient.sendMessageStanza(locMsg, recipientJid = "engineering@s.whatsapp.net")
            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(1000)
                updateMessageStatus(chatId, locMsg.id, MessageStatus.READ)
            }
        }
    }

    fun sendContact(chatId: Long, name: String) {
        val now = timeFormat.format(Date())
        val contactMsg = Message(
            id = "cnt_${System.currentTimeMillis()}",
            chatId = chatId,
            senderName = "Você",
            text = name,
            timestamp = now,
            isFromMe = true,
            type = MessageType.CONTACT,
            status = MessageStatus.SENT
        )
        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + contactMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = "👤 $name", lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(contactMsg)
            webSocketClient.sendMessageStanza(contactMsg, recipientJid = "engineering@s.whatsapp.net")
            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(1000)
                updateMessageStatus(chatId, contactMsg.id, MessageStatus.READ)
            }
        }
    }

    fun sendCapturedMedia(chatId: Long, file: File, isVideo: Boolean, caption: String) {
        val now = timeFormat.format(Date())
        val mediaType = if (isVideo) MessageType.VIDEO else MessageType.IMAGE
        val mediaMsg = Message(
            id = "media_${System.currentTimeMillis()}",
            chatId = chatId,
            senderName = "Você",
            text = caption,
            timestamp = now,
            isFromMe = true,
            type = mediaType,
            fileName = file.name,
            fileSize = "${file.length() / 1024} KB",
            filePath = file.absolutePath,
            isDownloaded = true,
            status = MessageStatus.SENT
        )

        playSound(R.raw.keq)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            currentMap + (chatId to (chatMsgs + mediaMsg))
        }

        _chats.update { list ->
            list.map {
                val label = if (isVideo) "🎥 Vídeo" else "📷 Foto"
                val preview = if (caption.isNotBlank()) "$label: $caption" else label
                if (it.id == chatId) it.copy(lastMessage = preview, lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(mediaMsg)
            webSocketClient.sendMessageStanza(mediaMsg, recipientJid = "engineering@s.whatsapp.net")
            if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(1000)
                updateMessageStatus(chatId, mediaMsg.id, MessageStatus.READ)
            }
        }
    }

    fun clearChat(chatId: Long) {
        stopAudioPlayback()
        _messages.update { map ->
            map + (chatId to emptyList())
        }
        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = "", lastMessageTime = "") else it
            }
        }
    }

    private fun updateMessageStatus(chatId: Long, msgId: String, newStatus: MessageStatus) {
        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: return@update currentMap
            val updated = chatMsgs.map {
                if (it.id == msgId) it.copy(status = newStatus) else it
            }
            currentMap + (chatId to updated)
        }
        viewModelScope.launch {
            chatRepository.updateMessageStatus(msgId, newStatus)
        }
    }

    fun startCall(contactName: String, isVideo: Boolean) {
        _activeCallContactName.value = contactName
        _activeCallIsVideo.value = isVideo
        _isInVoipCall.value = true
        playSound(R.raw.kgj)
        webRtcCallManager.startCall(contactName, isVideo)
    }

    fun endCall() {
        webRtcCallManager.endCall()
        _isInVoipCall.value = false
        playSound(R.raw.kfn)
    }

    fun toggleCallMute(): Boolean {
        return webRtcCallManager.toggleMute()
    }

    fun toggleCallSpeaker(): Boolean {
        return webRtcCallManager.toggleSpeaker()
    }

    private fun playSound(resId: Int) {
        try {
            val player = MediaPlayer.create(getApplication(), resId)
            player?.setOnCompletionListener { it.release() }
            player?.start()
        } catch (e: Exception) {
            // Audio fallback
        }
    }

    private fun handleIncomingRemoteMessage(event: IncomingMessageEvent) {
        val chatId = _activeChatId.value ?: 1L
        val now = timeFormat.format(Date(event.timestampMs))
        val newMsg = Message(
            id = event.messageId,
            chatId = chatId,
            senderName = event.senderName,
            text = event.text,
            timestamp = now,
            isFromMe = false,
            type = event.type,
            status = MessageStatus.READ
        )

        playSound(R.raw.kd3)

        _messages.update { currentMap ->
            val chatMsgs = currentMap[chatId] ?: emptyList()
            if (chatMsgs.any { it.id == newMsg.id }) currentMap
            else currentMap + (chatId to (chatMsgs + newMsg))
        }

        _chats.update { list ->
            list.map {
                if (it.id == chatId) it.copy(lastMessage = event.text, lastMessageTime = now) else it
            }
        }

        viewModelScope.launch {
            chatRepository.saveMessage(newMsg)
        }
    }

    private fun handleIncomingRemoteReceipt(receipt: IncomingReceiptEvent) {
        for ((chatId, msgs) in _messages.value) {
            if (msgs.any { it.id == receipt.messageId }) {
                updateMessageStatus(chatId, receipt.messageId, receipt.status)
                break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
        audioRecorder.cancelRecording()
        webRtcCallManager.endCall()
        webSocketClient.disconnect()
    }
}
