-- WhatsApp axolotl.db SQLite Schema
-- Mirrored from WhatsApp Signal Protocol & Post-Quantum Cryptography (PQC) storage

CREATE TABLE IF NOT EXISTS identities (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipient_id TEXT UNIQUE NOT NULL,
    registration_id INTEGER NOT NULL,
    public_key BLOB NOT NULL,
    private_key BLOB,
    next_prekey_id INTEGER NOT NULL DEFAULT 1,
    timestamp INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS fast_ratchet_sender_keys (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    device_id INTEGER NOT NULL DEFAULT 0,
    record BLOB NOT NULL,
    UNIQUE(group_id, sender_id, device_id)
);

CREATE TABLE IF NOT EXISTS kyber_prekeys (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    prekey_id INTEGER NOT NULL UNIQUE,
    sent_to_server INTEGER NOT NULL DEFAULT 0,
    kyber_public_key BLOB NOT NULL,
    kyber_private_key BLOB NOT NULL,
    signature BLOB NOT NULL,
    timestamp INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipient_id TEXT NOT NULL,
    device_id INTEGER NOT NULL DEFAULT 0,
    session_record BLOB NOT NULL,
    timestamp INTEGER NOT NULL,
    UNIQUE(recipient_id, device_id)
);
