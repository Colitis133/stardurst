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
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BubbleService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private View removeView;
    private WindowManager.LayoutParams bubbleParams;

    private View replyBubble, sendBubble, readBubble;
    private boolean isMenuOpen = false;

    private Animation glowAnimation;

    private final BroadcastReceiver bubbleEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.stardust.TTS_SPEAKING".equals(intent.getAction())) {
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
        setupMiniBubbles();

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

    private void setupMiniBubbles() {
        replyBubble = createMiniBubble(R.drawable.ic_reply, "reply");
        sendBubble = createMiniBubble(R.drawable.ic_send, "send_message");
        readBubble = createMiniBubble(R.drawable.ic_read, "read_message");
    }

    private View createMiniBubble(int iconRes, final String action) {
        ImageView miniBubble = new ImageView(this);
        miniBubble.setImageResource(iconRes);
        miniBubble.setVisibility(View.GONE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                bubbleParams.type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        windowManager.addView(miniBubble, params);

        miniBubble.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                triggerAction(action);
                toggleMenu(); // Close menu after action
            }
            return true;
        });

        return miniBubble;
    }


    private void toggleMenu() {
        isMenuOpen = !isMenuOpen;
        if (isMenuOpen) {
            // Position and show mini bubbles
            positionMiniBubble(replyBubble, -150, 0);
            positionMiniBubble(sendBubble, 0, -150);
            positionMiniBubble(readBubble, 150, 0);
        } else {
            replyBubble.setVisibility(View.GONE);
            sendBubble.setVisibility(View.GONE);
            readBubble.setVisibility(View.GONE);
        }
    }

    private void positionMiniBubble(View miniBubble, int xOffset, int yOffset) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) miniBubble.getLayoutParams();
        params.x = bubbleParams.x + xOffset;
        params.y = bubbleParams.y + yOffset;
        miniBubble.setVisibility(View.VISIBLE);
        windowManager.updateViewLayout(miniBubble, params);
    }


    private void setupRemoveView() {
        removeView = new FrameLayout(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                150,
                bubbleParams.type,
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
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastClickTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_UP:
                        removeView.setVisibility(View.GONE);
                        if (!isDragging) {
                            toggleMenu();
                        }
                        if (isBubbleOverRemoveView(bubbleParams.x, bubbleParams.y)) {
                            stopSelf();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            if (isMenuOpen) {
                                toggleMenu(); // Close menu when dragging starts
                            }
                            removeView.setVisibility(View.VISIBLE);
                            bubbleParams.x = initialX + (int) deltaX;
                            bubbleParams.y = initialY + (int) deltaY;
                            windowManager.updateViewLayout(bubbleView, bubbleParams);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private boolean isBubbleOverRemoveView(int x, int y) {
        int[] removeLocation = new int[2];
        removeView.getLocationOnScreen(removeLocation);
        return y > removeLocation[1] - bubbleView.getHeight();
    }

    private void triggerAction(String action) {
        new Thread(() -> {
            try {
                URL url = new URL("http://127.0.0.1:4000/action");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("action", action);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

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
        if (replyBubble != null) windowManager.removeView(replyBubble);
        if (sendBubble != null) windowManager.removeView(sendBubble);
        if (readBubble != null) windowManager.removeView(readBubble);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bubbleEventReceiver);
    }
}
