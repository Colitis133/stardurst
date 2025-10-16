package com.stardust;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BubbleService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private View removeView;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams removeParams;
    private Animation glowAnimation;

    private final BroadcastReceiver bubbleEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.stardust.TTS_SPEAKING".equals(action)) {
                boolean isSpeaking = intent.getBooleanExtra("isSpeaking", false);
                if (isSpeaking) {
                    bubbleView.startAnimation(glowAnimation);
                } else {
                    bubbleView.clearAnimation();
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        glowAnimation = AnimationUtils.loadAnimation(this, R.anim.bubble_glow);

        setupBubbleView();
        setupRemoveView();

        LocalBroadcastManager.getInstance(this).registerReceiver(bubbleEventReceiver, new IntentFilter("com.stardust.TTS_SPEAKING"));
    }

    private void setupBubbleView() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 0;
        bubbleParams.y = 100;

        windowManager.addView(bubbleView, bubbleParams);
        setBubbleTouchListener();
    }

    private void setupRemoveView() {
        removeView = new FrameLayout(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                150,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;
        ImageView removeIcon = new ImageView(this);
        removeIcon.setImageResource(R.drawable.ic_remove_bubble);
        ((FrameLayout) removeView).addView(removeIcon);
        removeView.setVisibility(View.GONE);
        windowManager.addView(removeView, params);
    }

    private void setBubbleTouchListener() {
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long lastClickTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastClickTime = System.currentTimeMillis();
                        removeView.setVisibility(View.VISIBLE);
                        return true;
                    case MotionEvent.ACTION_UP:
                        removeView.setVisibility(View.GONE);
                        if (System.currentTimeMillis() - lastClickTime < 200) {
                            triggerStt();
                        }
                        // Check if bubble is over remove view
                        if (isBubbleOverRemoveView(bubbleParams.x, bubbleParams.y)) {
                            stopSelf(); // Stop the service to remove the bubble
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        bubbleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        bubbleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleView, bubbleParams);
                        return true;
                }
                return false;
            }
        });
    }

    private boolean isBubbleOverRemoveView(int x, int y) {
        int[] removeLocation = new int[2];
        removeView.getLocationOnScreen(removeLocation);
        // A simple check, can be improved
        return y > removeLocation[1] - bubbleView.getHeight();
    }

    private void triggerStt() {
        // Asynchronously call the STT endpoint
        new Thread(() -> {
            try {
                URL url = new URL("http://127.0.0.1:4000/stt/start");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.getResponseCode(); // Fire and forget
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null) windowManager.removeView(bubbleView);
        if (removeView != null) windowManager.removeView(removeView);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bubbleEventReceiver);
    }
}
