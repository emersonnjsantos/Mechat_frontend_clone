# -*- coding: utf-8 -*-
"""
WhatsApp CLI Test Client
Simulates a second real device connecting to the server to chat with the Android app.
"""

import asyncio
import json
import sys
import time

try:
    import websockets
except ImportError:
    print("Please install websockets: pip install websockets")
    sys.exit(1)

SERVER_URL = "ws://localhost:8080"
MY_JID = "engineering@s.whatsapp.net"
TARGET_JID = "client_me@s.whatsapp.net"

async def test_client():
    print(f"Connecting to {SERVER_URL} as {MY_JID}...")
    async with websockets.connect(SERVER_URL) as ws:
        # Authenticate
        auth_msg = {
            "action": "auth",
            "jid": MY_JID
        }
        await ws.send(json.dumps(auth_msg))
        ack = await ws.recv()
        print(f"Auth response: {ack}")

        print("Listening for messages... (Press Ctrl+C to stop)")
        while True:
            raw = await ws.recv()
            data = json.loads(raw)
            print(f"\n[RECEIVED EVENT]: {json.dumps(data, indent=2)}")

            # If it's a message, acknowledge delivery and read
            if data.get("action") == "message":
                stanza = data.get("stanza", {})
                msg_id = stanza.get("message_id")
                sender = stanza.get("sender_jid")
                print(f"\n>>> Incoming Message from {sender}: '{stanza.get('text_content')}'")

                # Send READ receipt
                read_rcpt = {
                    "action": "receipt",
                    "receipt": {
                        "message_id": msg_id,
                        "recipient_jid": sender,
                        "status": "READ",
                        "timestamp": int(time.time() * 1000)
                    }
                }
                await ws.send(json.dumps(read_rcpt))
                print(f"Sent READ receipt for message {msg_id}")

if __name__ == "__main__":
    try:
        asyncio.run(test_client())
    except KeyboardInterrupt:
        print("\nTest client exited.")
