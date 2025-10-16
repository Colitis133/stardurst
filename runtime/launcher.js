const { makeWASocket, useMultiFileAuthState, fetchLatestBaileysVersion } = require('@whiskeysockets/baileys');
const fetch = require('node-fetch');
const WebSocket = require('ws');
const db = require('./cacheDB'); // sqlite wrapper
const { format } = require('date-fns');

const BRIDGE_URL = 'http://127.0.0.1:4000';
const BRIDGE_WS_URL = 'ws://127.0.0.1:4001/events';

// Simple state machine
const state = {
    awaiting: null, // null | 'reply_decision' | 'reply_content' | 'send_message_recipient' | 'send_message_content' | 'read_message_query'
    context: {} // stores message context like JID, message content, etc.
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

async function resolveRecipientToJid(recipient) {
    // Simple check for phone number format (e.g., starts with +, or is all digits)
    if (recipient.match(/^\+?\d+$/)) {
        // Assuming it's a number, append @s.whatsapp.net if not already there
        return recipient.includes('@s.whatsapp.net') ? recipient : `${recipient}@s.whatsapp.net`;
    } else {
        // Try to find JID by pushName from stored messages
        const jid = await db.getJidByPushName(recipient);
        if (jid) return jid;
    }
    return null; // Could not resolve
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
            } else if (state.awaiting === 'send_message_recipient') {
                const recipient = transcript.replace(/send message to |to /g, '').trim();
                if (recipient) {
                    const jid = await resolveRecipientToJid(recipient);
                    if (jid) {
                        state.context.recipientJid = jid;
                        state.context.recipientName = recipient;
                        state.awaiting = 'send_message_content';
                        await tts(`Okay, what message do you want to send to ${recipient}?`);
                        await startStt();
                    } else {
                        await tts(`Sorry, I couldn't find a contact named or numbered ${recipient}. Please try again.`);
                        state.awaiting = null;
                        state.context = {};
                    }
                } else {
                    await tts('I didn't catch the recipient. Please try again.');
                    state.awaiting = null;
                    state.context = {};
                }
            } else if (state.awaiting === 'send_message_content') {
                try {
                    await sock.sendMessage(state.context.recipientJid, { text: transcript });
                    await tts(`Message sent to ${state.context.recipientName}.`);
                } catch (e) {
                    await tts('Sorry, I failed to send the message.');
                    console.error('Failed to send message:', e);
                } finally {
                    state.awaiting = null;
                    state.context = {};
                }
            } else if (state.awaiting === 'read_message_query') {
                let contactMatch = transcript.match(/(from|for) (.*)/);
                let contact = contactMatch ? contactMatch[2].trim() : null;
                let limitMatch = transcript.match(/(last|latest) (\d+)/);
                let limit = limitMatch ? parseInt(limitMatch[2]) : 1;

                if (contact) {
                    const jid = await resolveRecipientToJid(contact);
                    if (jid) {
                        const messages = await db.getMessagesByJid(jid, limit);
                        if (messages.length > 0) {
                            let responseText = `The last ${messages.length} messages from ${contact} are: `;
                            messages.forEach((msg, index) => {
                                responseText += `Message ${index + 1} at ${msg.time}: ${msg.text}. `;
                            });
                            await tts(responseText);
                        } else {
                            await tts(`No messages found from ${contact}.`);
                        }
                    } else {
                        await tts(`Sorry, I couldn't find a contact named or numbered ${contact}.`);
                    }
                } else {
                    await tts('I didn't catch the contact. Please try again.');
                }
                state.awaiting = null;
                state.context = {};
            }
        } else if (evt.event === 'ui_action') {
            switch (evt.action) {
                case 'reply':
                    // This case is for explicitly tapping 'reply' from the menu
                    // The automatic reply flow from messages.upsert already sets state.awaiting
                    // So, if we are not already in a reply flow, we can initiate it.
                    if (state.awaiting === null) {
                        await tts("Do you want to reply to the last message?");
                        state.awaiting = 'reply_decision';
                        // Need to set context.jid here if not already set by an incoming message
                        // For now, assume context.jid is set by the last incoming message.
                        // A more robust solution would involve storing the last message's JID persistently.
                        await startStt();
                    } else {
                        await tts("I'm already in a conversation flow. Please finish that first.");
                    }
                    break;
                case 'send_message':
                    await tts("Who do you want to send a message to? Please say their name or number.");
                    state.awaiting = 'send_message_recipient';
                    await startStt();
                    break;
                case 'read_message':
                    await tts("I'm listening. What message do you want me to read? For example, 'last message from John' or 'last two messages from 2348133859014'.");
                    state.awaiting = 'read_message_query';
                    await startStt();
                    break;
            }
        }
    }

}

start().catch(console.error);
