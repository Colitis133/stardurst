package com.stardust;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionExplanationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build UI programmatically to avoid adding a new layout resource
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scroll.setLayoutParams(scrollParams);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Permissions & Privacy");
        title.setTextSize(20);
        title.setPadding(0,0,0,12);
        container.addView(title);

        TextView body = new TextView(this);
        body.setText(
                "Why Stardust asks for permissions:\n\n" +
                "• Microphone (RECORD_AUDIO): Stardust only listens when you explicitly trigger a voice action (for example, when you tap reply and it asks \"What's your reply?\").\n\n" +
                "• Overlay/Bubble: The floating bubble provides quick access to reply, send and read features. You can hide or disable it anytime.\n\n" +
                "• Foreground service & notifications: We run a foreground service so the app can keep the WhatsApp runtime alive and show the mic/listening state.\n\n" +
                "Privacy & logs:\n\n" +
                "• Local-only logs: Message text and runtime logs are stored locally on your device (under the app's files) and are not uploaded to any external server. You can inspect logs from the app's Log Viewer.\n\n" +
                "• Stored messages: The app keeps a local cache of recent messages to implement read and contextual reply flows. You can clear the cache from the app settings (or uninstall the app to remove all app data).\n\n" +
                "• Credentials: WhatsApp auth credentials are stored on-device. The app uses the Android Keystore (or should be configured to) to protect secrets — ensure this is enabled in app settings if available.\n\n" +
                "If you deny microphone permission, voice features will be disabled but you can still use the bubble UI to type messages. If you have concerns, you can revoke permissions anytime from the system settings."
        );
        body.setTextSize(14);
        container.addView(body);

        Button openSettings = new Button(this);
        openSettings.setText("Open App Settings");
        openSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        container.addView(openSettings);

        Button viewLogs = new Button(this);
        viewLogs.setText("View Local Logs");
        viewLogs.setOnClickListener(v -> {
            try {
                Intent i = new Intent(this, Class.forName("com.stardust.LogViewerActivity"));
                startActivity(i);
            } catch (ClassNotFoundException e) {
                // Activity not found — ignore
                e.printStackTrace();
            }
        });
        container.addView(viewLogs);

        Button close = new Button(this);
        close.setText("Close");
        close.setOnClickListener(v -> finish());
        container.addView(close);

        scroll.addView(container);
        setContentView(scroll);
    }
}