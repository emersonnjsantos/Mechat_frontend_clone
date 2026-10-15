-- WhatsApp msgstore.db SQLite Schema
-- Extracted and mirrored from WhatsApp v2.26.39.79

CREATE TABLE IF NOT EXISTS jid (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    user TEXT NOT NULL,
    server TEXT NOT NULL DEFAULT 's.whatsapp.net',
    agent INTEGER DEFAULT 0,
    device_id INTEGER DEFAULT 0,
    raw_string TEXT UNIQUE NOT NULL
);

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
    ephemeral_expiration INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_row_id INTEGER NOT NULL REFERENCES chat(_id),
    from_me INTEGER NOT NULL DEFAULT 1,
    key_id TEXT NOT NULL UNIQUE,
    sender_jid_row_id INTEGER REFERENCES jid(_id),
    timestamp INTEGER NOT NULL,
    status INTEGER NOT NULL DEFAULT 0, -- 0=pending, 1=sent, 2=delivered, 3=read
    message_type INTEGER NOT NULL DEFAULT 0, -- 0=text, 1=image, 2=audio, 3=video, 4=doc
    text_data TEXT,
    starred INTEGER DEFAULT 0,
    quoted_row_id INTEGER REFERENCES messages(_id),
    ephemeral_duration INTEGER DEFAULT 0
);

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

CREATE INDEX IF NOT EXISTS idx_messages_chat_row_id ON messages(chat_row_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);
