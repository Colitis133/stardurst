package com.stardust;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BridgeService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val ttsQueue = LinkedList<Pair<String, String>>()
    private var isTtsInitialized = false
    private val sockets = ConcurrentHashMap<WebSocketSession, WebSocketSession>()
    private val sttResultChannel = Channel<String>(Channel.UNLIMITED)

    private val ktorServer by lazy {
        embeddedServer(Netty, port = 4000, host = "127.0.0.1") {
            install(ContentNegotiation) {
                gson { }
            }
            install(WebSockets)

            import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

class BridgeService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
// ... existing code ...
            routing {
                post("/tts") {
                    val body = call.receive<Map<String, String>>()
                    val text = body["text"] ?: "No text provided"
                    val utteranceId = UUID.randomUUID().toString()
                    ttsQueue.add(Pair(text, utteranceId))
                    if (isTtsInitialized && !tts.isSpeaking) {
                        speakNext()
                    }
                    call.respond(mapOf("status" to "queued", "id" to utteranceId))
                }

                post("/stt/start") {
                    startListening()
                    call.respond(mapOf("status" to "listening"))
                }

                post("/action") {
                    val body = call.receive<Map<String, String>>()
                    val action = body["action"] ?: "unknown"
                    val message = JSONObject().apply {
                        put("event", "ui_action")
                        put("action", action)
                    }.toString()

                    sockets.keys.forEach { session ->
                        session.send(Frame.Text(message))
                    }
                    call.respond(mapOf("status" to "action_received", "action" to action))
                }

                post("/secure/encrypt") {
                    val body = call.receive<Map<String, String>>()
                    val data = body["data"]
                    if (data == null) {
                        call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "No data provided"))
                        return@post
                    }
                    val encrypted = KeystoreHelper.encrypt(data)
                    call.respond(mapOf("encrypted" to encrypted))
                }

                post("/secure/decrypt") {
                    val body = call.receive<Map<String, String>>()
                    val encrypted = body["encrypted"]
                    if (encrypted == null) {
                        call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "No encrypted data provided"))
                        return@post
                    }
                    try {
                        val decrypted = KeystoreHelper.decrypt(encrypted)
                        call.respond(mapOf("data" to decrypted))
                    } catch (e: Exception) {
                        call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Decryption failed"))
                    }
                }

                webSocket("/events") {
                    sockets[this] = this
                    try {
// ... existing code ...
        }
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(SttListener(sttResultChannel))

        Thread { ktorServer.start(wait = true) }.start()
    }

    private fun speakNext() {
        if (ttsQueue.isNotEmpty()) {
            val (text, utteranceId) = ttsQueue.poll()
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    private fun broadcastTtsState(isSpeaking: Boolean) {
        val intent = Intent("com.stardust.TTS_SPEAKING")
        intent.putExtra("isSpeaking", isSpeaking)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun speakNext() {
        if (ttsQueue.isNotEmpty()) {
            val (text, utteranceId) = ttsQueue.poll()
            broadcastTtsState(true)
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    private fun startListening() {
// ... existing code ...
        speechRecognizer.startListening(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts.language = Locale.US
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    broadcastTtsState(true)
                }
                override fun onDone(utteranceId: String?) {
                    if (ttsQueue.isEmpty()) {
                        broadcastTtsState(false)
                    }
                    speakNext()
                }
                override fun onError(utteranceId: String?) {
                    broadcastTtsState(false)
                }
            })
            speakNext() // Start speaking if anything was queued before init
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
// ... existing code ...

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts.language = Locale.US
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    speakNext()
                }
                override fun onError(utteranceId: String?) {}
            })
            speakNext() // Start speaking if anything was queued before init
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
        ktorServer.stop(1000, 2000)
    }
}
