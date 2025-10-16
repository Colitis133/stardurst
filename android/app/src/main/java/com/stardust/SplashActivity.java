package com.stardust;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.Settings;
import android.os.Build;
import android.net.Uri;
import android.os.PowerManager;
import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPermissionsAndProceed();
            }
        }, 1000); // 1-second delay to show splash
    }

    private void checkPermissionsAndProceed() {
        boolean recordAudioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean batteryOptimizationIgnored = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            batteryOptimizationIgnored = pm.isIgnoringBatteryOptimizations(getPackageName());
        }

        if (!recordAudioGranted || !overlayGranted || !batteryOptimizationIgnored) {
            // If any permission is missing, go to explanation activity
            Intent intent = new Intent(SplashActivity.this, PermissionExplanationActivity.class);
            startActivity(intent);
            finish();
        } else {
            // All permissions granted, proceed to main app
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
