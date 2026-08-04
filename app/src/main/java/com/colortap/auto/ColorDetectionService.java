package com.colortap.auto;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.util.Random;

public class ColorDetectionService extends Service {
    private static final String CHANNEL_ID = "ColorTapChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private WindowManager windowManager;
    private View overlayView;
    private TextView statusText, countText;
    private Handler handler = new Handler();
    private Random random = new Random();
    
    private int baseColor = Color.YELLOW;
    private int additionalColor = Color.BLUE;
    private int resetColor = Color.WHITE;
    private int targetSequenceCount = 10;
    private int tapCount = 1;
    
    private int currentSequenceCount = 0;
    private boolean isRunning = false;
    private boolean hasTapped = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        createOverlayView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            baseColor = intent.getIntExtra("baseColor", Color.YELLOW);
            additionalColor = intent.getIntExtra("additionalColor", Color.BLUE);
            resetColor = intent.getIntExtra("resetColor", Color.WHITE);
            targetSequenceCount = intent.getIntExtra("targetSequenceCount", 10);
            tapCount = intent.getIntExtra("tapCount", 1);
        }
        
        isRunning = true;
        currentSequenceCount = 0;
        hasTapped = false;
        startDetection();
        return START_STICKY;
    }
    
    private void createOverlayView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_view, null);
        statusText = overlayView.findViewById(R.id.statusText);
        countText = overlayView.findViewById(R.id.countText);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    handleTouch(event.getRawX(), event.getRawY());
                }
                return false;
            }
        });
        
        windowManager.addView(overlayView, params);
        updateUI("Ready", 0);
    }
    
    private void startDetection() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    int detectedColor = simulateColorDetection();
                    processDetectedColor(detectedColor);
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }
    
    private int simulateColorDetection() {
        int[] colors = {baseColor, additionalColor, resetColor, Color.GREEN, Color.RED};
        return colors[random.nextInt(colors.length)];
    }
    
    private void processDetectedColor(int detectedColor) {
        boolean isBaseOrAdditional = isColorMatch(detectedColor, baseColor) || 
                                     isColorMatch(detectedColor, additionalColor);
        boolean isResetColor = isColorMatch(detectedColor, resetColor);
        
        if (isResetColor) {
            currentSequenceCount = 0;
            hasTapped = false;
            updateUI("🔄 Reset Color Detected", currentSequenceCount);
            return;
        }
        
        if (isBaseOrAdditional) {
            currentSequenceCount++;
            updateUI("✅ Color Detected", currentSequenceCount);
            
            if (currentSequenceCount >= targetSequenceCount && !hasTapped) {
                performTaps();
                hasTapped = true;
                currentSequenceCount = 0;
                updateUI("🎯 Tapped! Resetting...", 0);
                
                handler.postDelayed(() -> {
                    hasTapped = false;
                    updateUI("🔄 Ready", 0);
                }, 2000);
            }
        } else {
            updateUI("⏳ Different Color", currentSequenceCount);
        }
    }
    
    private boolean isColorMatch(int color1, int color2) {
        int threshold = 30;
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        
        return Math.abs(r1 - r2) < threshold &&
               Math.abs(g1 - g2) < threshold &&
               Math.abs(b1 - b2) < threshold;
    }
    
    private void performTaps() {
        updateUI("🔨 Tapping " + tapCount + " times...", currentSequenceCount);
        
        for (int i = 0; i < tapCount; i++) {
            final int tapNumber = i + 1;
            handler.postDelayed(() -> {
                updateUI("🔨 Tap " + tapNumber + "/" + tapCount, currentSequenceCount);
            }, i * 500);
        }
    }
    
    private void handleTouch(float x, float y) {
        // Color detection logic here
    }
    
    private void updateUI(String status, int count) {
        handler.post(() -> {
            if (statusText != null) {
                statusText.setText(status);
            }
            if (countText != null) {
                countText.setText("Count: " + count + "/" + targetSequenceCount);
            }
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "ColorTap Auto Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Color Detection Service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ColorTap Auto")
            .setContentText("Color detection is running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        handler.removeCallbacksAndMessages(null);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
