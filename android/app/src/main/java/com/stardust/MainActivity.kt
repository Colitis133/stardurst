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
            package com.stardust

            // Stub to avoid duplicate class definitions. Real implementation is in MainActivity.java
            object MainActivityStub {}
                        String qrString = intent.getStringExtra("data");
