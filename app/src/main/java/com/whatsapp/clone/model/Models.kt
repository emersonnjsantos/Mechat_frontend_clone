package com.whatsapp.clone.model

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ
}

enum class MessageType {
    TEXT,
    AUDIO,
    IMAGE,
    VIDEO,
    DOCUMENT,
    LOCATION,
    CONTACT,
    SYSTEM_E2EE_NOTICE
}

data class Message(
    val id: String,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.READ,
    val type: MessageType = MessageType.TEXT,
    val audioDurationSeconds: Int = 0,
    val waveform: List<Float> = emptyList(),
    val isEphemeral: Boolean = false,
    val fileName: String? = null,
    val fileSize: String? = null,
    val isDownloaded: Boolean = true,
    val isDownloading: Boolean = false,
    val filePath: String? = null
)

data class Chat(
    val id: Long,
    val contactName: String,
    val avatarLetter: String,
    val avatarColor: Long,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isOnline: Boolean = false
)

data class CallItem(
    val id: Long,
    val contactName: String,
    val avatarLetter: String,
    val avatarColor: Long,
    val timestamp: String,
    val isIncoming: Boolean,
    val isMissed: Boolean,
    val isVideo: Boolean
)
