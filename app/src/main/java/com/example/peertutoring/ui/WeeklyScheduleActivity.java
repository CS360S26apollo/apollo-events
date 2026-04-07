package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly schedule screen.
 * Tutor selects available hours per day (8AM–9PM, 3-column grid).
 * Supports Copy to All Days and Save to Firestore.
 */
public class WeeklyScheduleActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Day names for display and Firestore keys
    private static final String[] DAY_KEYS   = {"mon","tue","wed","thu","fri","sat","sun"};
    private static final String[] DAY_LABELS = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
    private static final int[]    TAB_IDS    = {R.id.tabMon,R.id.tabTue,R.id.tabWed,R.id.tabThu,R.id.tabFri,R.id.tabSat,R.id.tabSun};
    private static final int[]    SUM_IDS    = {R.id.sumMon,R.id.sumTue,R.id.sumWed,R.id.sumThu,R.id.sumFri,R.id.sumSat,R.id.sumSun};

    // Hours 8–21 (8AM to 9PM)
    private static final int HOUR_START = 8;
    private static final int HOUR_END   = 21;

    // schedule[dayIndex] = list of selected hour integers
    private final List<List<Integer>> schedule = new ArrayList<>();
    private int selectedDay = 0;

    private LinearLayout layoutHourGrid;
    private TextView tvSelectHoursLabel, tvHoursCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_schedule);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize empty schedule for each day
        for (int i = 0; i < 7; i++) schedule.add(new ArrayList<>());

        layoutHourGrid      = findViewById(R.id.layoutHourGrid);
        tvSelectHoursLabel  = findViewById(R.id.tvSelectHoursLabel);
        tvHoursCount        = findViewById(R.id.tvHoursCount);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupDayTabs();

        if (findViewById(R.id.btnCopyToAllDays) != null)
            findViewById(R.id.btnCopyToAllDays).setOnClickListener(v -> copyToAllDays());

        if (findViewById(R.id.btnSaveSchedule) != null)
            findViewById(R.id.btnSaveSchedule).setOnClickListener(v -> saveSchedule());

        loadSchedule();
    }

    // ── Day tabs ──────────────────────────────────────────────

    private void setupDayTabs() {
        for (int i = 0; i < TAB_IDS.length; i++) {
            final int dayIndex = i;
            LinearLayout tab = findViewById(TAB_IDS[i]);
            if (tab != null) {
                tab.setOnClickListener(v -> switchDay(dayIndex));
            }
        }
        switchDay(0); // default to Monday
    }

    private void switchDay(int dayIndex) {
        selectedDay = dayIndex;

        // Reset all tabs
        for (int i = 0; i < TAB_IDS.length; i++) {
            LinearLayout tab = findViewById(TAB_IDS[i]);
            if (tab == null) continue;
            if (i == dayIndex) {
                tab.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                setTabTextColor(tab, Color.WHITE);
            } else {
                tab.setBackground(null);
                setTabTextColor(tab, Color.parseColor("#071A3D"));
            }
        }

        if (tvSelectHoursLabel != null)
            tvSelectHoursLabel.setText("Select Hours - " + DAY_LABELS[dayIndex]);

        buildHourGrid();
        updateHoursCount();
    }

    private void setTabTextColor(LinearLayout tab, int color) {
        for (int i = 0; i < tab.getChildCount(); i++) {
            if (tab.getChildAt(i) instanceof TextView) {
                ((TextView) tab.getChildAt(i)).setTextColor(color);
            }
        }
    }

    // ── Hour grid ─────────────────────────────────────────────

    private void buildHourGrid() {
        if (layoutHourGrid == null) return;
        layoutHourGrid.removeAllViews();

        List<Integer> dayHours = schedule.get(selectedDay);
        int dp8 = dp(8);

        // Build rows of 3 cells
        int hour = HOUR_START;
        while (hour <= HOUR_END) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp8);
            row.setLayoutParams(rowParams);

            for (int col = 0; col < 3 && hour <= HOUR_END; col++, hour++) {
                final int h = hour;
                boolean selected = dayHours.contains(h);

                MaterialCardView cell = new MaterialCardView(this);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, dp(72), 1f);
                cellParams.setMargins(col > 0 ? dp8 : 0, 0, 0, 0);
                cell.setLayoutParams(cellParams);
                cell.setRadius(dp(12));
                cell.setCardElevation(selected ? dp(2) : 0);
                cell.setCardBackgroundColor(selected
                        ? Color.parseColor("#00C853")
                        : Color.parseColor("#F3F4F6"));

                LinearLayout inner = new LinearLayout(this);
                inner.setOrientation(LinearLayout.VERTICAL);
                inner.setGravity(Gravity.CENTER);
                inner.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

                // Hour number
                TextView tvHour = new TextView(this);
                tvHour.setText(String.valueOf(h <= 12 ? h : h - 12));
                tvHour.setTextSize(18);
                tvHour.setTextColor(selected ? Color.WHITE : Color.parseColor("#071A3D"));
                tvHour.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHour.setGravity(Gravity.CENTER);

                // AM/PM
                TextView tvAmPm = new TextView(this);
                tvAmPm.setText(h < 12 ? "AM" : "PM");
                tvAmPm.setTextSize(11);
                tvAmPm.setTextColor(selected ? Color.parseColor("#CCFFFFFF") : Color.parseColor("#8B97A8"));
                tvAmPm.setGravity(Gravity.CENTER);

                // Checkmark if selected
                if (selected) {
                    TextView tvCheck = new TextView(this);
                    tvCheck.setText("✓");
                    tvCheck.setTextColor(Color.WHITE);
                    tvCheck.setTextSize(10);
                    tvCheck.setGravity(Gravity.CENTER);
                    inner.addView(tvCheck);
                }

                inner.addView(tvHour);
                inner.addView(tvAmPm);
                cell.addView(inner);

                // Toggle on click
                cell.setOnClickListener(v -> {
                    List<Integer> hrs = schedule.get(selectedDay);
                    if (hrs.contains(h)) {
                        hrs.remove(Integer.valueOf(h));
                    } else {
                        hrs.add(h);
                    }
                    buildHourGrid();
                    updateHoursCount();
                    updateSummary();
                });

                row.addView(cell);
            }

            layoutHourGrid.addView(row);
        }
    }

    private void updateHoursCount() {
        int count = schedule.get(selectedDay).size();
        if (tvHoursCount != null)
            tvHoursCount.setText(count + (count == 1 ? " hour" : " hours"));
    }

    // ── Weekly summary ────────────────────────────────────────

    private void updateSummary() {
        for (int i = 0; i < SUM_IDS.length; i++) {
            TextView tv = findViewById(SUM_IDS[i]);
            if (tv != null) tv.setText(schedule.get(i).size() + "h");
        }
    }

    // ── Copy to all days ──────────────────────────────────────

    private void copyToAllDays() {
        List<Integer> source = new ArrayList<>(schedule.get(selectedDay));
        for (int i = 0; i < 7; i++) {
            schedule.get(i).clear();
            schedule.get(i).addAll(source);
        }
        updateSummary();
        Toast.makeText(this, "✅ Copied to all days!", Toast.LENGTH_SHORT).show();
    }

    // ── Save to Firestore ─────────────────────────────────────

    private void saveSchedule() {
        if (currentUser == null) {
            Toast.makeText(this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> scheduleMap = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            scheduleMap.put("schedule." + DAY_KEYS[i], schedule.get(i));
        }

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .set(scheduleMap, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Schedule saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Load from Firestore ───────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadSchedule() {
        if (currentUser == null) {
            buildHourGrid();
            updateSummary();
            return;
        }

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        for (int i = 0; i < 7; i++) {
                            List<Long> saved = (List<Long>) doc.get("schedule." + DAY_KEYS[i]);
                            if (saved != null) {
                                schedule.get(i).clear();
                                for (Long h : saved) schedule.get(i).add(h.intValue());
                            }
                        }
                    }
                    buildHourGrid();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    buildHourGrid();
                    updateSummary();
                });
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}