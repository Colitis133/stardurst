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
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class RuntimeService extends Service {
    private static final String CHANNEL_ID = "StardustRuntimeChannel";
    private Process nodeProcess;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stardust Runtime")
                .setContentText("WhatsApp assistant is running.")
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's icon
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            try {
                File runtimeDir = new File(getFilesDir(), "runtime");
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "bootstrap.sh");
                pb.directory(runtimeDir);
                pb.redirectErrorStream(true);
                nodeProcess = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Simple check if the line is a JSON object from our runtime
                    if (line.startsWith("STARDUST_EVENT:")) {
                        try {
                            JSONObject json = new JSONObject(line.substring("STARDUST_EVENT:".length()));
                            Intent localIntent = new Intent("stardust-runtime-event");
                            localIntent.putExtra("event", json.getString("event"));
                            if (json.has("data")) {
                                localIntent.putExtra("data", json.getString("data"));
                            }
                            LocalBroadcastManager.getInstance(RuntimeService.this).sendBroadcast(localIntent);
                        } catch (Exception e) {
                            // Not a valid JSON event, just log it
                            System.out.println("Node.js: " + line);
                        }
                    } else {
                        System.out.println("Node.js: " + line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (nodeProcess != null) {
            nodeProcess.destroy();
        }
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
