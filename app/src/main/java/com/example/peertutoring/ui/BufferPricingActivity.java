package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.peertutoring.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class BufferPricingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private int selectedRate    = 100;
    private int selectedBuffer  = 15;
    private int selectedSession = 60;
    private int selectedNotice  = 24;

    private static final int[] RATE_BTN_IDS = {
            R.id.btnRate50, R.id.btnRate75, R.id.btnRate100,
            R.id.btnRate150, R.id.btnRate200, R.id.btnRate300
    };
    private static final int[] RATE_VALUES = {50, 75, 100, 150, 200, 300};

    private static final int[] BUFFER_BTN_IDS = {
            R.id.btnBuffer5, R.id.btnBuffer10, R.id.btnBuffer15,
            R.id.btnBuffer30, R.id.btnBuffer45, R.id.btnBuffer60
    };
    private static final int[] BUFFER_VALUES = {5, 10, 15, 30, 45, 60};

    private static final int[] SESSION_BTN_IDS = {
            R.id.btnSession30, R.id.btnSession45, R.id.btnSession60,
            R.id.btnSession90, R.id.btnSession120
    };
    private static final int[] SESSION_VALUES = {30, 45, 60, 90, 120};

    private static final int[] NOTICE_BTN_IDS = {
            R.id.btnNotice2, R.id.btnNotice6, R.id.btnNotice12,
            R.id.btnNotice24, R.id.btnNotice48, R.id.btnNotice72
    };
    private static final int[] NOTICE_VALUES = {2, 6, 12, 24, 48, 72};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buffer_pricing);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupRateButtons();
        setupBufferSeekBar();
        setupBufferQuickButtons();
        setupSessionButtons();
        setupNoticeButtons();

        if (findViewById(R.id.btnSaveSettings) != null)
            findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void setupRateButtons() {
        for (int i = 0; i < RATE_BTN_IDS.length; i++) {
            final int rate = RATE_VALUES[i];
            Button btn = findViewById(RATE_BTN_IDS[i]);
            if (btn != null) btn.setOnClickListener(v -> selectRate(rate));
        }
        updateRateUI();
    }

    private void selectRate(int rate) {
        selectedRate = rate;
        updateRateUI();
        updateEarnings();
    }

    private void updateRateUI() {
        TextView tvRate = findViewById(R.id.tvSelectedRate);
        if (tvRate != null) tvRate.setText(selectedRate + " Tokens");

        for (int i = 0; i < RATE_BTN_IDS.length; i++) {
            Button btn = findViewById(RATE_BTN_IDS[i]);
            if (btn == null) continue;
            if (RATE_VALUES[i] == selectedRate) {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btn.setTextColor(Color.parseColor("#6200EA"));
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
                btn.setTextColor(Color.parseColor("#CCFFFFFF"));
                btn.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void updateEarnings() {
        // Daily = rate * 4h, Weekly = rate * 20h, Monthly = rate * 80h
        int daily   = selectedRate * 4;
        int weekly  = selectedRate * 20;
        int monthly = selectedRate * 80;

        setTextIfExists(R.id.tvEarningsDaily,         String.valueOf(daily));
        setTextIfExists(R.id.tvEarningsDailyLabel,    "Daily (4h)");
        setTextIfExists(R.id.tvEarningsWeekly,        String.valueOf(weekly));
        setTextIfExists(R.id.tvEarningsWeeklyLabel,   "Weekly (20h)");
        setTextIfExists(R.id.tvEarningsMonthly,       String.valueOf(monthly));
        setTextIfExists(R.id.tvEarningsMonthlyLabel,  "Monthly (80h)");
    }

    private void setupBufferSeekBar() {
        SeekBar seekBar = findViewById(R.id.seekBarBuffer);
        if (seekBar == null) return;

        // max=55, maps to 5-60 minutes (step 5)
        seekBar.setMax(55);
        seekBar.setProgress(selectedBuffer - 5);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    // Snap to nearest 5
                    int snapped = Math.round(progress / 5f) * 5 + 5;
                    selectedBuffer = Math.min(60, Math.max(5, snapped));
                    updateBufferUI();
                    updateBufferButtonStyles();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setupBufferQuickButtons() {
        for (int i = 0; i < BUFFER_BTN_IDS.length; i++) {
            final int val = BUFFER_VALUES[i];
            Button btn = findViewById(BUFFER_BTN_IDS[i]);
            if (btn != null) btn.setOnClickListener(v -> {
                selectedBuffer = val;
                updateBufferUI();
                updateBufferButtonStyles();
                SeekBar sb = findViewById(R.id.seekBarBuffer);
                if (sb != null) sb.setProgress(val - 5);
            });
        }
        updateBufferButtonStyles();
    }

    private void updateBufferUI() {
        setTextIfExists(R.id.tvBufferLabel, "Selected: " + selectedBuffer + " minutes");
        setTextIfExists(R.id.tvBufferBadge, selectedBuffer + "min");
    }

    private void updateBufferButtonStyles() {
        for (int i = 0; i < BUFFER_BTN_IDS.length; i++) {
            Button btn = findViewById(BUFFER_BTN_IDS[i]);
            if (btn == null) continue;
            if (BUFFER_VALUES[i] == selectedBuffer) {
                btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                btn.setTextColor(Color.WHITE);
                btn.setBackgroundTintList(null);
            } else {
                btn.setBackground(null);
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
                btn.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
    }

    private void setupSessionButtons() {
        for (int i = 0; i < SESSION_BTN_IDS.length; i++) {
            final int val = SESSION_VALUES[i];
            Button btn = findViewById(SESSION_BTN_IDS[i]);
            if (btn != null) btn.setOnClickListener(v -> {
                selectedSession = val;
                updateSessionButtonStyles();
            });
        }
        updateSessionButtonStyles();
    }

    private void updateSessionButtonStyles() {
        for (int i = 0; i < SESSION_BTN_IDS.length; i++) {
            Button btn = findViewById(SESSION_BTN_IDS[i]);
            if (btn == null) continue;
            if (SESSION_VALUES[i] == selectedSession) {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00C853")));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
                btn.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
    }

    private void setupNoticeButtons() {
        for (int i = 0; i < NOTICE_BTN_IDS.length; i++) {
            final int val = NOTICE_VALUES[i];
            Button btn = findViewById(NOTICE_BTN_IDS[i]);
            if (btn != null) btn.setOnClickListener(v -> {
                selectedNotice = val;
                updateNoticeButtonStyles();
            });
        }
        updateNoticeButtonStyles();
    }

    private void updateNoticeButtonStyles() {
        for (int i = 0; i < NOTICE_BTN_IDS.length; i++) {
            Button btn = findViewById(NOTICE_BTN_IDS[i]);
            if (btn == null) continue;
            if (NOTICE_VALUES[i] == selectedNotice) {
                btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                btn.setTextColor(Color.WHITE);
                btn.setBackgroundTintList(null);
            } else {
                btn.setBackground(null);
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
                btn.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
    }

    private void saveSettings() {
        Map<String, Object> data = new HashMap<>();
        data.put("hourlyRate",      selectedRate);
        data.put("bufferMinutes",   selectedBuffer);
        data.put("sessionLength",   selectedSession);
        data.put("bookingNoticeH",  selectedNotice);

        if (currentUser == null) {
            Toast.makeText(this, "✅ Settings saved successfully!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Settings saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadSettings() {
        if (currentUser == null) {
            updateEarnings();
            updateBufferUI();
            return;
        }

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long rate    = doc.getLong("hourlyRate");
                        Long buffer  = doc.getLong("bufferMinutes");
                        Long session = doc.getLong("sessionLength");
                        Long notice  = doc.getLong("bookingNoticeH");
                        if (rate    != null) selectedRate    = rate.intValue();
                        if (buffer  != null) selectedBuffer  = buffer.intValue();
                        if (session != null) selectedSession = session.intValue();
                        if (notice  != null) selectedNotice  = notice.intValue();
                    }
                    updateRateUI();
                    updateEarnings();
                    updateBufferUI();
                    updateBufferButtonStyles();
                    updateSessionButtonStyles();
                    updateNoticeButtonStyles();

                    SeekBar sb = findViewById(R.id.seekBarBuffer);
                    if (sb != null) sb.setProgress(selectedBuffer - 5);
                })
                .addOnFailureListener(e -> {
                    updateEarnings();
                    updateBufferUI();
                });
    }

    private void setTextIfExists(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }
}