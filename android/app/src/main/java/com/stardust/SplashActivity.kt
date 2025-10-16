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

        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndProceed, 1000); // 1-second delay to show splash
package com.stardust

// Kotlin stub; actual implementation exists as SplashActivity.java
object SplashActivityStub {}
        }
    }
}
