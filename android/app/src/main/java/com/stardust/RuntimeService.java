package com.stardust;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import org.json.JSONObject;
import com.nodejsmobile.nodejs.NodeJs;
import com.nodejsmobile.nodejs.NodeJsMessageListener;

public class RuntimeService extends Service {
    private static final String CHANNEL_ID = "StardustRuntimeChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stardust Runtime")
                .setContentText("WhatsApp assistant is running.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!NodeJs.isNodeJsRunning()) {
            NodeJs.startNodeWithArguments(new String[]{});
        }

        NodeJs.addNodeJsMessageListener(new NodeJsMessageListener() {
            @Override
            public void onMessage(String message) {
                if (message.startsWith("STARDUST_EVENT:")) {
                    try {
                        JSONObject json = new JSONObject(message.substring("STARDUST_EVENT:".length()));
                        Intent localIntent = new Intent("stardust-runtime-event");
                        localIntent.putExtra("event", json.getString("event"));
                        if (json.has("data")) {
                            localIntent.putExtra("data", json.getString("data"));
                        }
                        LocalBroadcastManager.getInstance(RuntimeService.this).sendBroadcast(localIntent);
                    } catch (Exception e) {
                        Log.e("Node.js", "Error parsing STARDUST_EVENT: " + e.getMessage());
                    }
                } else {
                    Log.i("Node.js", message);
                }
            }

            @Override
            public void onExit(int exitCode) {
                Log.i("Node.js", "Node.js exited with code: " + exitCode);
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (NodeJs.isNodeJsRunning()) {
            NodeJs.stopNode();
        }
        NodeJs.removeNodeJsMessageListener(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stardust Runtime Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
