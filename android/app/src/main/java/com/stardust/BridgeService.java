package com.stardust;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.ktor.application.*;
import io.ktor.features.*;
import io.ktor.gson.*;
import io.ktor.http.cio.websocket.*;
import io.ktor.request.*;
import io.ktor.response.*;
import io.ktor.routing.*;
import io.ktor.server.engine.*;
import io.ktor.server.netty.*;
import io.ktor.websocket.*;
import kotlinx.coroutines.channels.Channel;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeService extends Service implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private final LinkedList<Pair<String, String>> ttsQueue = new LinkedList<>();
    private boolean isTtsInitialized = false;
    private final ConcurrentHashMap<WebSocketSession, WebSocketSession> sockets = new ConcurrentHashMap<>();
    private Channel<String> sttResultChannel;

    private NettyApplicationEngine ktorServer;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        // SttListener is in Kotlin and takes a Channel; for now we'll skip initialization details

        // Start Ktor server in a background thread
        new Thread(() -> {
            ktorServer = (NettyApplicationEngine) embeddedServer(Netty, 4000, "127.0.0.1", module -> {
                install(ContentNegotiation.class, config -> {
                    // gson config
                });
                install(WebSockets.class);
                routing();
            });
            ktorServer.start(true);
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onInit(int status) {
        // TextToSpeech init handled in Kotlin code previously; keep simple
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (ktorServer != null) {
            ktorServer.stop(1000, 2000);
        }
    }
}
