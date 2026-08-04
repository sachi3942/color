package com.colortap.auto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQUEST = 100;
    
    private EditText targetCountInput, tapCountInput;
    private Button baseColorBtn, additionalColorBtn, resetColorBtn;
    private TextView baseColorDisplay, additionalColorDisplay, resetColorDisplay;
    private Button startBtn, stopBtn;
    private TextView statusText;
    
    private int baseColor = Color.YELLOW;
    private int additionalColor = Color.BLUE;
    private int resetColor = Color.WHITE;
    private int targetSequenceCount = 10;
    private int tapCount = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        checkPermissions();
    }
    
    private void initViews() {
        targetCountInput = findViewById(R.id.targetCountInput);
        tapCountInput = findViewById(R.id.tapCountInput);
        
        baseColorBtn = findViewById(R.id.baseColorBtn);
        additionalColorBtn = findViewById(R.id.additionalColorBtn);
        resetColorBtn = findViewById(R.id.resetColorBtn);
        
        baseColorDisplay = findViewById(R.id.baseColorDisplay);
        additionalColorDisplay = findViewById(R.id.additionalColorDisplay);
        resetColorDisplay = findViewById(R.id.resetColorDisplay);
        
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        statusText = findViewById(R.id.statusText);
        
        targetCountInput.setText("10");
        tapCountInput.setText("1");
        updateColorDisplays();
    }
    
    private void setupListeners() {
        baseColorBtn.setOnClickListener(v -> showColorPicker("Base Color", color -> {
            baseColor = color;
            updateColorDisplays();
        }));
        
        additionalColorBtn.setOnClickListener(v -> showColorPicker("Additional Color", color -> {
            additionalColor = color;
            updateColorDisplays();
        }));
        
        resetColorBtn.setOnClickListener(v -> showColorPicker("Reset Color", color -> {
            resetColor = color;
            updateColorDisplays();
        }));
        
        startBtn.setOnClickListener(v -> startDetection());
        stopBtn.setOnClickListener(v -> stopDetection());
    }
    
    private void showColorPicker(String title, ColorSelectionListener listener) {
        String[] colorNames = {"Red", "Yellow", "Blue", "Green", "White", "Black", "Purple", "Orange"};
        int[] colorValues = {
            Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN,
            Color.WHITE, Color.BLACK, Color.rgb(128, 0, 128), Color.rgb(255, 165, 0)
        };
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select " + title)
            .setItems(colorNames, (dialog, which) -> {
                listener.onColorSelected(colorValues[which]);
            })
            .show();
    }
    
    private void updateColorDisplays() {
        baseColorDisplay.setBackgroundColor(baseColor);
        baseColorDisplay.setText(getColorName(baseColor));
        baseColorDisplay.setTextColor(getContrastColor(baseColor));
        
        additionalColorDisplay.setBackgroundColor(additionalColor);
        additionalColorDisplay.setText(getColorName(additionalColor));
        additionalColorDisplay.setTextColor(getContrastColor(additionalColor));
        
        resetColorDisplay.setBackgroundColor(resetColor);
        resetColorDisplay.setText(getColorName(resetColor));
        resetColorDisplay.setTextColor(getContrastColor(resetColor));
    }
    
    private String getColorName(int color) {
        if (color == Color.RED) return "Red";
        if (color == Color.YELLOW) return "Yellow";
        if (color == Color.BLUE) return "Blue";
        if (color == Color.GREEN) return "Green";
        if (color == Color.WHITE) return "White";
        if (color == Color.BLACK) return "Black";
        if (color == Color.rgb(128, 0, 128)) return "Purple";
        if (color == Color.rgb(255, 165, 0)) return "Orange";
        return "Custom";
    }
    
    private int getContrastColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
                return false;
            }
        }
        return true;
    }
    
    private void startDetection() {
        if (!checkPermissions()) {
            Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            targetSequenceCount = Integer.parseInt(targetCountInput.getText().toString());
            tapCount = Integer.parseInt(tapCountInput.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent serviceIntent = new Intent(this, ColorDetectionService.class);
        serviceIntent.putExtra("baseColor", baseColor);
        serviceIntent.putExtra("additionalColor", additionalColor);
        serviceIntent.putExtra("resetColor", resetColor);
        serviceIntent.putExtra("targetSequenceCount", targetSequenceCount);
        serviceIntent.putExtra("tapCount", tapCount);
        
        startService(serviceIntent);
        statusText.setText("🟢 Service Running");
        statusText.setTextColor(Color.GREEN);
        Toast.makeText(this, "Color Detection Started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDetection() {
        Intent serviceIntent = new Intent(this, ColorDetectionService.class);
        stopService(serviceIntent);
        statusText.setText("🔴 Service Stopped");
        statusText.setTextColor(Color.RED);
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    interface ColorSelectionListener {
        void onColorSelected(int color);
    }
}
