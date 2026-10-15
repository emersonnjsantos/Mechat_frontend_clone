package com.whatsapp.clone.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.whatsapp.clone.model.Chat
import com.whatsapp.clone.model.Message
import com.whatsapp.clone.model.MessageStatus
import com.whatsapp.clone.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {

    private val dbHelper = WhatsAppDatabaseHelper.getInstance(context)
    private val TAG = "ChatRepository"

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM chat", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()

        if (count == 0) {
            Log.i(TAG, "Seeding initial WhatsApp chats and messages into msgstore.db...")
            db.beginTransaction()
            try {
                // 1. Insert JIDs
                val jid1Id = insertJid(db, "engineering", "s.whatsapp.net", "engineering@s.whatsapp.net")
                val jid2Id = insertJid(db, "family", "g.us", "family@g.us")
                val jid3Id = insertJid(db, "rea_provider", "s.whatsapp.net", "rea_provider@s.whatsapp.net")

                // 2. Insert Chats
                val chat1Id = insertChatRecord(
                    db, jid1Id, "Engenharia de Software (Meta / WA)",
                    "Microfone e áudio real ativados!", "18:52", 2, isPinned = true,
                    avatarLetter = "E", avatarColor = 0xFF00A884.toLong()
                )
                val chat2Id = insertChatRecord(
                    db, jid2Id, "Família & Amigos",
                    "Toque no microfone para gravar sua voz real", "17:30", 1, isPinned = false,
                    avatarLetter = "F", avatarColor = 0xFF128C7E.toLong()
                )
                val chat3Id = insertChatRecord(
                    db, jid3Id, "Provedor Seguro REA (Artifact Graph)",
                    "Criptografia Pós-Quântica Crystals-Kyber verificada com sucesso.", "16:15", 0, isPinned = false,
                    avatarLetter = "R", avatarColor = 0xFF2A5298.toLong()
                )

                // 3. Insert Messages for Chat 1
                insertMessageRecord(
                    db, chat1Id, "msg_0", "System",
                    "As mensagens e chamadas são protegidas com a criptografia de ponta a ponta (Signal Protocol & Crystals-Kyber PQC).",
                    "18:00", System.currentTimeMillis() - 7200000,
                    isFromMe = false, status = MessageStatus.READ, type = MessageType.SYSTEM_E2EE_NOTICE
                )
                insertMessageRecord(
                    db, chat1Id, "msg_1", "Engenharia de Software",
                    "Agora o microfone grava sua voz real pelo hardware físico do celular!",
                    "18:45", System.currentTimeMillis() - 3600000,
                    isFromMe = false, status = MessageStatus.READ, type = MessageType.TEXT
                )
                insertMessageRecord(
                    db, chat1Id, "msg_doc_1", "Engenharia de Software",
                    "", "18:46", System.currentTimeMillis() - 3500000,
                    isFromMe = false, status = MessageStatus.READ, type = MessageType.DOCUMENT,
                    fileName = "WhatsApp_Relatorio_Engenharia_Reversa.pdf", fileSize = "2.4 MB", isDownloaded = false
                )
                insertMessageRecord(
                    db, chat1Id, "msg_2", "Você",
                    "Perfeito! Todas as linguagens (Kotlin, Java, C++, C, SQL, Protobuf) e banco SQLite msgstore.db integrados.",
                    "18:50", System.currentTimeMillis() - 1800000,
                    isFromMe = true, status = MessageStatus.READ, type = MessageType.TEXT
                )

                // Messages for Chat 2
                insertMessageRecord(
                    db, chat2Id, "msg_fam_0", "Família & Amigos",
                    "Olá! Testando a nova versão com banco de dados local SQLite.",
                    "17:20", System.currentTimeMillis() - 7600000,
                    isFromMe = false, status = MessageStatus.READ, type = MessageType.TEXT
                )

                // Messages for Chat 3
                insertMessageRecord(
                    db, chat3Id, "msg_rea_0", "Provedor Seguro REA",
                    "Criptografia Pós-Quântica Crystals-Kyber verificada com sucesso.",
                    "16:15", System.currentTimeMillis() - 10000000,
                    isFromMe = false, status = MessageStatus.READ, type = MessageType.TEXT
                )

                db.setTransactionSuccessful()
                Log.i(TAG, "Database seeded successfully with initial records!")
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding initial data", e)
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun insertJid(db: android.database.sqlite.SQLiteDatabase, user: String, server: String, raw: String): Long {
        val cv = ContentValues().apply {
            put("user", user)
            put("server", server)
            put("raw_string", raw)
        }
        return db.insertWithOnConflict("jid", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun insertChatRecord(
        db: android.database.sqlite.SQLiteDatabase,
        jidRowId: Long,
        subject: String,
        lastMsg: String,
        lastTime: String,
        unreadCount: Int,
        isPinned: Boolean,
        avatarLetter: String,
        avatarColor: Long
    ): Long {
        val cv = ContentValues().apply {
            put("jid_row_id", jidRowId)
            put("subject", subject)
            put("created_timestamp", System.currentTimeMillis())
            put("unseen_message_count", unreadCount)
            put("pinned", if (isPinned) 1 else 0)
            put("avatar_letter", avatarLetter)
            put("avatar_color", avatarColor)
        }
        return db.insert("chat", null, cv)
    }

    private fun insertMessageRecord(
        db: android.database.sqlite.SQLiteDatabase,
        chatId: Long,
        keyId: String,
        senderName: String,
        text: String,
        timeStr: String,
        timestamp: Long,
        isFromMe: Boolean,
        status: MessageStatus,
        type: MessageType,
        audioDuration: Int = 0,
        waveform: String = "",
        fileName: String? = null,
        fileSize: String? = null,
        filePath: String? = null,
        isDownloaded: Boolean = true
    ): Long {
        val cv = ContentValues().apply {
            put("chat_row_id", chatId)
            put("key_id", keyId)
            put("sender_name", senderName)
            put("text_data", text)
            put("timestamp_str", timeStr)
            put("timestamp", timestamp)
            put("from_me", if (isFromMe) 1 else 0)
            put("status", status.ordinal)
            put("message_type", type.ordinal)
            put("audio_duration_seconds", audioDuration)
            put("waveform_data", waveform)
            put("file_name", fileName)
            put("file_size", fileSize)
            put("file_path", filePath)
            put("is_downloaded", if (isDownloaded) 1 else 0)
        }
        return db.insertWithOnConflict("messages", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getAllChats(): List<Chat> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Chat>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c._id, c.subject, c.unseen_message_count, c.pinned, c.avatar_letter, c.avatar_color,
                   (SELECT m.text_data FROM messages m WHERE m.chat_row_id = c._id ORDER BY m.timestamp DESC LIMIT 1) as last_text,
                   (SELECT m.timestamp_str FROM messages m WHERE m.chat_row_id = c._id ORDER BY m.timestamp DESC LIMIT 1) as last_time
            FROM chat c
            ORDER BY c.pinned DESC, c._id ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val subject = cursor.getString(1) ?: "Contato"
            val unread = cursor.getInt(2)
            val pinned = cursor.getInt(3) == 1
            val letter = cursor.getString(4) ?: subject.take(1).uppercase()
            val color = cursor.getLong(5)
            val lastMsg = cursor.getString(6) ?: ""
            val lastTime = cursor.getString(7) ?: ""

            list.add(
                Chat(
                    id = id,
                    contactName = subject,
                    avatarLetter = letter,
                    avatarColor = if (color != 0L) color else 0xFF00A884,
                    lastMessage = lastMsg,
                    lastMessageTime = lastTime,
                    unreadCount = unread,
                    isPinned = pinned,
                    isOnline = id == 1L || id == 3L
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun getMessagesForChat(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Message>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "messages",
            null,
            "chat_row_id = ?",
            arrayOf(chatId.toString()),
            null,
            null,
            "timestamp ASC, _id ASC"
        )

        while (cursor.moveToNext()) {
            val keyId = cursor.getString(cursor.getColumnIndexOrThrow("key_id"))
            val senderName = cursor.getString(cursor.getColumnIndexOrThrow("sender_name")) ?: ""
            val text = cursor.getString(cursor.getColumnIndexOrThrow("text_data")) ?: ""
            val timeStr = cursor.getString(cursor.getColumnIndexOrThrow("timestamp_str")) ?: ""
            val fromMe = cursor.getInt(cursor.getColumnIndexOrThrow("from_me")) == 1
            val statusOrd = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
            val typeOrd = cursor.getInt(cursor.getColumnIndexOrThrow("message_type"))
            val audioDuration = cursor.getInt(cursor.getColumnIndexOrThrow("audio_duration_seconds"))
            val waveformStr = cursor.getString(cursor.getColumnIndexOrThrow("waveform_data")) ?: ""
            val fileName = cursor.getString(cursor.getColumnIndexOrThrow("file_name"))
            val fileSize = cursor.getString(cursor.getColumnIndexOrThrow("file_size"))
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow("file_path"))
            val isDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow("is_downloaded")) == 1

            val status = MessageStatus.entries.getOrElse(statusOrd) { MessageStatus.READ }
            val type = MessageType.entries.getOrElse(typeOrd) { MessageType.TEXT }

            val waveformList = if (waveformStr.isNotBlank()) {
                waveformStr.split(",").mapNotNull { it.toFloatOrNull() }
            } else {
                emptyList()
            }

            list.add(
                Message(
                    id = keyId,
                    chatId = chatId,
                    senderName = senderName,
                    text = text,
                    timestamp = timeStr,
                    isFromMe = fromMe,
                    status = status,
                    type = type,
                    audioDurationSeconds = audioDuration,
                    waveform = waveformList,
                    fileName = fileName,
                    fileSize = fileSize,
                    filePath = filePath,
                    isDownloaded = isDownloaded
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun saveMessage(message: Message) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val waveformStr = message.waveform.joinToString(",")
        val cv = ContentValues().apply {
            put("chat_row_id", message.chatId)
            put("key_id", message.id)
            put("sender_name", message.senderName)
            put("text_data", message.text)
            put("timestamp_str", message.timestamp)
            put("timestamp", System.currentTimeMillis())
            put("from_me", if (message.isFromMe) 1 else 0)
            put("status", message.status.ordinal)
            put("message_type", message.type.ordinal)
            put("audio_duration_seconds", message.audioDurationSeconds)
            put("waveform_data", waveformStr)
            put("file_name", message.fileName)
            put("file_size", message.fileSize)
            put("file_path", message.filePath)
            put("is_downloaded", if (message.isDownloaded) 1 else 0)
        }
        db.insertWithOnConflict("messages", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun updateMessageStatus(keyId: String, newStatus: MessageStatus) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("status", newStatus.ordinal)
        }
        db.update("messages", cv, "key_id = ?", arrayOf(keyId))
    }

    suspend fun clearUnreadCount(chatId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("unseen_message_count", 0)
        }
        db.update("chat", cv, "_id = ?", arrayOf(chatId.toString()))
    }
}
