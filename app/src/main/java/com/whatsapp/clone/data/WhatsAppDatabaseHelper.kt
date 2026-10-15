package com.whatsapp.clone.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class WhatsAppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "msgstore.db"
        const val DATABASE_VERSION = 1
        private const val TAG = "WhatsAppDbHelper"

        // Singleton instance
        @Volatile
        private var instance: WhatsAppDatabaseHelper? = null

        fun getInstance(context: Context): WhatsAppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: WhatsAppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Creating WhatsApp SQLite tables (msgstore.db and axolotl.db schemas)...")
        
        // --- MSGSTORE TABLES ---
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS jid (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                user TEXT NOT NULL,
                server TEXT NOT NULL DEFAULT 's.whatsapp.net',
                agent INTEGER DEFAULT 0,
                device_id INTEGER DEFAULT 0,
                raw_string TEXT UNIQUE NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS chat (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                jid_row_id INTEGER NOT NULL REFERENCES jid(_id),
                subject TEXT,
                created_timestamp INTEGER NOT NULL,
                unseen_message_count INTEGER DEFAULT 0,
                last_message_row_id INTEGER,
                last_read_message_row_id INTEGER,
                archived INTEGER DEFAULT 0,
                pinned INTEGER DEFAULT 0,
                ephemeral_expiration INTEGER DEFAULT 0,
                avatar_letter TEXT DEFAULT 'W',
                avatar_color INTEGER DEFAULT -16734076
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS messages (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_row_id INTEGER NOT NULL REFERENCES chat(_id),
                from_me INTEGER NOT NULL DEFAULT 1,
                key_id TEXT NOT NULL UNIQUE,
                sender_name TEXT,
                timestamp INTEGER NOT NULL,
                timestamp_str TEXT NOT NULL,
                status INTEGER NOT NULL DEFAULT 0,
                message_type INTEGER NOT NULL DEFAULT 0,
                text_data TEXT,
                starred INTEGER DEFAULT 0,
                quoted_row_id INTEGER,
                ephemeral_duration INTEGER DEFAULT 0,
                audio_duration_seconds INTEGER DEFAULT 0,
                waveform_data TEXT,
                file_name TEXT,
                file_size TEXT,
                file_path TEXT,
                is_downloaded INTEGER DEFAULT 1
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS message_media (
                message_row_id INTEGER PRIMARY KEY REFERENCES messages(_id),
                chat_row_id INTEGER NOT NULL REFERENCES chat(_id),
                mime_type TEXT NOT NULL,
                file_size INTEGER NOT NULL,
                file_path TEXT,
                file_hash TEXT,
                duration INTEGER DEFAULT 0,
                waveform BLOB,
                width INTEGER DEFAULT 0,
                height INTEGER DEFAULT 0
            );
        """.trimIndent())

        // --- AXOLOTL (E2EE & PQC) TABLES ---
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS identities (
                recipient_id TEXT PRIMARY KEY,
                registration_id INTEGER NOT NULL,
                public_key BLOB NOT NULL,
                private_key BLOB,
                next_prekey_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS prekeys (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                key_id INTEGER UNIQUE NOT NULL,
                public_key BLOB NOT NULL,
                private_key BLOB NOT NULL,
                sent_to_server INTEGER DEFAULT 0,
                direct_distribution INTEGER DEFAULT 0
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS signed_prekeys (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                key_id INTEGER UNIQUE NOT NULL,
                public_key BLOB NOT NULL,
                private_key BLOB NOT NULL,
                signature BLOB NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sessions (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipient_id TEXT NOT NULL,
                device_id INTEGER NOT NULL,
                record_name TEXT NOT NULL,
                session_record BLOB NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS kyber_prekeys (
                prekey_id INTEGER PRIMARY KEY,
                public_key BLOB NOT NULL,
                private_key BLOB NOT NULL,
                signature BLOB NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """.trimIndent())

        // --- INDICES ---
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_chat_row_id ON messages(chat_row_id);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_key_id ON messages(key_id);")

        Log.i(TAG, "WhatsApp SQLite database initialized successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS message_media")
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS chat")
        db.execSQL("DROP TABLE IF EXISTS jid")
        db.execSQL("DROP TABLE IF EXISTS identities")
        db.execSQL("DROP TABLE IF EXISTS prekeys")
        db.execSQL("DROP TABLE IF EXISTS signed_prekeys")
        db.execSQL("DROP TABLE IF EXISTS sessions")
        db.execSQL("DROP TABLE IF EXISTS kyber_prekeys")
        onCreate(db)
    }
}
