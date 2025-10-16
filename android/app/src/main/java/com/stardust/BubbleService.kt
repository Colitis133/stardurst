package com.stardust

// Kotlin stub to replace malformed Java content. Java implementation exists in BubbleService.java
object BubbleServiceStub {}
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
        unregisterReceiver(bubbleEventReceiver);
    }
}
