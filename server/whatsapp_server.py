# -*- coding: utf-8 -*-
"""
WhatsApp Real-time Wire Server (FunXMPP / WebSocket Gateway)
Implements Phase 2: Real-time Message Routing, Delivery Receipts and Multi-device Sync.
"""

import asyncio
import json
import logging
import sys
import time
from typing import Dict, Set

try:
    import websockets
except ImportError:
    print("Error: websockets library is required. Install it using: pip install websockets")
    sys.exit(1)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%H:%M:%S'
)
logger = logging.getLogger("WhatsAppServer")

# Connected clients registry: jid -> WebSocketServerProtocol
connected_clients: Dict[str, websockets.WebSocketServerProtocol] = {}
# Offline message queues: jid -> list of message stanzas
offline_queues: Dict[str, list] = {}

PORT = 8080
HOST = "0.0.0.0"

async def broadcast_presence(jid: str, is_online: bool):
    """Notify relevant contacts of online/offline status."""
    presence_msg = {
        "action": "presence",
        "jid": jid,
        "is_online": is_online,
        "timestamp": int(time.time() * 1000)
    }
    raw = json.dumps(presence_msg)
    for other_jid, ws in connected_clients.items():
        if other_jid != jid:
            try:
                await ws.send(raw)
            except Exception:
                pass

async def handle_client(websocket, path=None):
    client_jid = None
    client_ip = websocket.remote_address[0] if websocket.remote_address else "unknown"
    logger.info(f"Incoming connection from {client_ip}")

    try:
        async for message in websocket:
            try:
                data = json.loads(message)
            except Exception as e:
                logger.warning(f"Failed to parse message: {e}")
                continue

            action = data.get("action")

            # 1. AUTH / REGISTRATION
            if action == "auth" or action == "login":
                client_jid = data.get("jid") or f"client_{int(time.time())}@s.whatsapp.net"
                connected_clients[client_jid] = websocket
                logger.info(f"Client authenticated: {client_jid} (Total online: {len(connected_clients)})")

                # Send ack back
                ack = {
                    "action": "auth_ack",
                    "status": "authenticated",
                    "server_version": "WhatsApp-Core-2.26.39.79-WireGateway",
                    "assigned_jid": client_jid,
                    "timestamp": int(time.time() * 1000)
                }
                await websocket.send(json.dumps(ack))
                await broadcast_presence(client_jid, True)

                # Deliver any pending offline messages
                if client_jid in offline_queues and offline_queues[client_jid]:
                    pending = offline_queues.pop(client_jid)
                    logger.info(f"Delivering {len(pending)} queued messages to {client_jid}")
                    for queued_msg in pending:
                        await websocket.send(json.dumps(queued_msg))

            # 2. MESSAGE STANZA ROUTING
            elif action == "message":
                stanza = data.get("stanza", {})
                msg_id = stanza.get("message_id") or f"msg_{int(time.time()*1000)}"
                sender = stanza.get("sender_jid") or client_jid or "anonymous@s.whatsapp.net"
                recipient = stanza.get("recipient_jid") or "engineering@s.whatsapp.net"
                text = stanza.get("text_content") or ""
                msg_type = stanza.get("type", "TEXT")

                logger.info(f"[{msg_type}] {sender} -> {recipient}: {text[:50]}")

                # Send IMMEDIATE SENT receipt back to sender
                sent_receipt = {
                    "action": "receipt",
                    "receipt": {
                        "message_id": msg_id,
                        "recipient_jid": recipient,
                        "status": "SENT",
                        "timestamp": int(time.time() * 1000)
                    }
                }
                await websocket.send(json.dumps(sent_receipt))

                # Route to recipient
                forward_packet = {
                    "action": "message",
                    "stanza": {
                        **stanza,
                        "message_id": msg_id,
                        "sender_jid": sender,
                        "recipient_jid": recipient,
                        "timestamp_ms": int(time.time() * 1000)
                    }
                }

                if recipient in connected_clients:
                    recipient_ws = connected_clients[recipient]
                    try:
                        await recipient_ws.send(json.dumps(forward_packet))
                        logger.info(f"Directly routed {msg_id} to {recipient}")

                        # Notify sender of DELIVERY
                        delivered_receipt = {
                            "action": "receipt",
                            "receipt": {
                                "message_id": msg_id,
                                "recipient_jid": recipient,
                                "status": "DELIVERED",
                                "timestamp": int(time.time() * 1000)
                            }
                        }
                        await websocket.send(json.dumps(delivered_receipt))
                    except Exception as e:
                        logger.error(f"Failed to forward message to {recipient}: {e}")
                        offline_queues.setdefault(recipient, []).append(forward_packet)
                else:
                    # Queue for offline recipient
                    offline_queues.setdefault(recipient, []).append(forward_packet)
                    logger.info(f"Recipient {recipient} offline. Queued message {msg_id}")

                    # Simulated server automated responder for core bots if recipient has no external client
                    if recipient in ["engineering@s.whatsapp.net", "core@s.whatsapp.net", "1"]:
                        asyncio.create_task(
                            handle_automated_reply(websocket, msg_id, sender, recipient, text)
                        )

            # 3. DELIVERY / READ RECEIPTS
            elif action == "receipt":
                receipt = data.get("receipt", {})
                target_jid = receipt.get("recipient_jid") or receipt.get("sender_jid")
                msg_id = receipt.get("message_id")
                status = receipt.get("status")
                logger.info(f"Receipt: {msg_id} status={status} to {target_jid}")

                if target_jid in connected_clients:
                    try:
                        await connected_clients[target_jid].send(json.dumps(data))
                    except Exception:
                        pass

            # 4. PING / HEARTBEAT
            elif action == "ping":
                pong = {"action": "pong", "timestamp": int(time.time() * 1000)}
                await websocket.send(json.dumps(pong))

    except websockets.exceptions.ConnectionClosed:
        logger.info(f"Connection closed for {client_jid or client_ip}")
    except Exception as e:
        logger.error(f"Error handling client {client_jid}: {e}")
    finally:
        if client_jid and client_jid in connected_clients:
            del connected_clients[client_jid]
            logger.info(f"Client disconnected: {client_jid} (Remaining: {len(connected_clients)})")
            await broadcast_presence(client_jid, False)

async def handle_automated_reply(sender_ws, original_msg_id: str, sender_jid: str, bot_jid: str, text: str):
    """Simulates WhatsApp core server automated responses with double checks and replies."""
    await asyncio.sleep(0.5)
    # 1. Send DELIVERED receipt
    deliv = {
        "action": "receipt",
        "receipt": {
            "message_id": original_msg_id,
            "recipient_jid": bot_jid,
            "status": "DELIVERED",
            "timestamp": int(time.time() * 1000)
        }
    }
    try:
        await sender_ws.send(json.dumps(deliv))
    except Exception:
        return

    await asyncio.sleep(0.8)
    # 2. Send READ receipt (double blue check)
    read_rcpt = {
        "action": "receipt",
        "receipt": {
            "message_id": original_msg_id,
            "recipient_jid": bot_jid,
            "status": "READ",
            "timestamp": int(time.time() * 1000)
        }
    }
    try:
        await sender_ws.send(json.dumps(read_rcpt))
    except Exception:
        return

    await asyncio.sleep(1.0)
    # 3. Send Bot Reply Stanza
    reply_stanza = {
        "action": "message",
        "stanza": {
            "message_id": f"server_reply_{int(time.time()*1000)}",
            "sender_jid": bot_jid,
            "sender_name": "Engenharia de Software (Meta / WA)",
            "recipient_jid": sender_jid,
            "timestamp_ms": int(time.time() * 1000),
            "type": "TEXT",
            "status": "READ",
            "text_content": f"[WebSocket Gateway] Mensagem '{text[:30]}' recebida e processada em tempo real com sucesso!"
        }
    }
    try:
        await sender_ws.send(json.dumps(reply_stanza))
        logger.info(f"Automated bot reply dispatched to {sender_jid}")
    except Exception:
        pass

async def main():
    logger.info("=" * 65)
    logger.info("  WHATSAPP REAL-TIME WIRE SERVER (WEBSOCKET & PROTOBUF GATEWAY)")
    logger.info("=" * 65)
    logger.info(f"Starting server on ws://{HOST}:{PORT} ...")

    async with websockets.serve(handle_client, HOST, PORT):
        logger.info(f"Server is listening on ws://localhost:{PORT}")
        logger.info("Android Emulator endpoint: ws://10.0.2.2:8080")
        logger.info("Physical Device endpoint: ws://<YOUR_LOCAL_IP>:8080")
        logger.info("Ready to route real-time messages between devices.")
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Server stopped by user.")
