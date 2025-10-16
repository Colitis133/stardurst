const { makeWASocket, useMultiFileAuthState, fetchLatestBaileysVersion } = require('@whiskeysockets/baileys');
const fetch = require('node-fetch');
const WebSocket = require('ws');
const db = require('./cacheDB'); // sqlite wrapper
const { format } = require('date-fns');

const BRIDGE_URL = 'http://127.0.0.1:4000';
const BRIDGE_WS_URL = 'ws://127.0.0.1:4001/events';

// Simple state machine
const state = {
    awaiting: null, // null | 'reply_decision' | 'reply_content'
    context: {} // stores message context like JID
};

function sendEventToUi(event) {
  console.log(`STARDUST_EVENT:${JSON.stringify(event)}`);
}

async function tts(text) {
    try {
        await fetch(`${BRIDGE_URL}/tts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
        });
    } catch (e) {
        console.error('Failed to call TTS bridge:', e.message);
    }
}

async function startStt() {
    try {
        await fetch(`${BRIDGE_URL}/stt/start`, { method: 'POST' });
    } catch (e) {
        console.error('Failed to call STT bridge:', e.message);
    }
}

function normalize(msg) {
    if (!msg.message) return null;

    // Expanded logic to find text content in various message types
    const messageContent = msg.message.conversation ||
                           msg.message.extendedTextMessage?.text ||
                           msg.message.imageMessage?.caption ||
                           msg.message.videoMessage?.caption ||
                           '';

    // If there's no text content after checking all types, we can ignore it.
    if (!messageContent) return null;

    const timestamp = new Date(msg.messageTimestamp * 1000);

    return {
        id: msg.key.id,
        jid: msg.key.remoteJid,
        pushName: msg.pushName || 'Someone', // Fallback for unknown contacts
        timestamp: msg.messageTimestamp,
        time: format(timestamp, 'h:mm a'),
        text: messageContent,
        isGroup: !!msg.key.remoteJid.endsWith('@g.us'),
        chatName: msg.key.remoteJid,
        direction: 'inbound'
    };
}

async function start() {
    const { state: authState, saveCreds } = await useMultiFileAuthState('./auth');
    const { version } = await fetchLatestBaileysVersion();
    const sock = makeWASocket({
        version,
        auth: authState,
        printQRInTerminal: false // We'll handle QR via the bridge
    });

    sock.ev.on('creds.update', saveCreds);

    const ws = new WebSocket(BRIDGE_WS_URL);
    ws.on('message', msg => handleBridgeEvent(JSON.parse(msg)));
    ws.on('open', () => console.log('Connected to bridge WebSocket'));
    ws.on('error', (err) => console.error('Bridge WebSocket error:', err.message));

    sock.ev.on('messages.upsert', async ({ messages }) => {
        for (const msg of messages) {
            if (msg.key.fromMe || !msg.message) continue;

            const summary = normalize(msg);
            if (!summary || !summary.text) continue;

            db.insertMessage(summary);

            const ttsText = `New message from ${summary.pushName} at ${summary.time}. It says: ${summary.text}. Do you want to reply?`;
            await tts(ttsText);

            state.awaiting = 'reply_decision';
            state.context = { jid: summary.jid };
            await startStt();
        }
    });

    sock.ev.on('connection.update', ({ connection, qr }) => {
        if (qr) {
            sendEventToUi({ event: 'qr', data: qr });
        }
        if (connection === 'close') {
            sendEventToUi({ event: 'disconnected' });
            // Implement reconnect logic here
        } else if (connection === 'open') {
            sendEventToUi({ event: 'connected' });
        }
    });

    async function handleBridgeEvent(evt) {
        if (evt.event === 'stt_result' && evt.final) {
            const transcript = evt.text.toLowerCase();

            if (state.awaiting === 'reply_decision') {
                if (transcript.includes('yes')) {
                    await tts("What's your reply?");
                    state.awaiting = 'reply_content';
                    await startStt();
                } else {
                    await tts('Okay, skipping.');
                    state.awaiting = null;
                    state.context = {};
                }
            } else if (state.awaiting === 'reply_content') {
                try {
                    await sock.sendMessage(state.context.jid, { text: evt.text });
                    await tts('Message sent.');
                } catch (e) {
                    await tts('Sorry, I failed to send the message.');
                    console.error('Failed to send message:', e);
                } finally {
                    state.awaiting = null;
                    state.context = {};
                }
            }
        }
    }
}

start().catch(console.error);
