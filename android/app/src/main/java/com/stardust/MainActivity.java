package com.stardust;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private TextView statusTextView;
    private Button linkDeviceButton;

    private BroadcastReceiver runtimeEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
            if (event != null) {
                switch (event) {
                    case "qr":
                        String qrString = intent.getStringExtra("data");
                        if (qrString != null) {
                            byte[] decodedString = Base64.decode(qrString, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            qrCodeImageView.setImageBitmap(decodedByte);
                            qrCodeImageView.setVisibility(View.VISIBLE);
                            statusTextView.setText("Status: Scan QR Code");
                        }
                        break;
                    case "connected":
                        qrCodeImageView.setVisibility(View.GONE);
                        statusTextView.setText("Status: Connected");
                        linkDeviceButton.setVisibility(View.GONE);
                        // Start the bubble service only when connected
                        startService(new Intent(MainActivity.this, BubbleService.class));
                        break;
                    case "disconnected":
                        statusTextView.setText("Status: Not Connected");
                        linkDeviceButton.setVisibility(View.VISIBLE);
                        // Stop the bubble service when disconnected
                        stopService(new Intent(MainActivity.this, BubbleService.class));
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        statusTextView = findViewById(R.id.statusTextView);
        linkDeviceButton = findViewById(R.id.linkDeviceButton);

        linkDeviceButton.setOnClickListener(v -> {
            // This will eventually send a message to the runtime to generate a QR code
            statusTextView.setText("Status: Generating QR Code...");
            // For now, we assume the runtime will send a "qr" event
        });

        // Start the core services
        Intent bridgeIntent = new Intent(this, BridgeService.class);
        startService(bridgeIntent);

        Intent runtimeIntent = new Intent(this, RuntimeService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(runtimeIntent);
        } else {
            startService(runtimeIntent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(runtimeEventReceiver, new IntentFilter("stardust-runtime-event"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(runtimeEventReceiver);
    }
}
