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
 * Activity for tutors to define their recurring weekly availability.
 * Role: Scheduling View for User Story 13 (Tutor Availability).
 * Purpose: Provides an interactive grid where tutors select available time slots 
 * for each day of the week, allowing students to book sessions accordingly.
 * 
 * Design Pattern: View-Controller managing complex state (Grid selection).
 * 
 * Outstanding Issues:
 * - Currently only supports 1-hour increments.
 * - No validation against existing booked sessions when clearing availability.
 */
public class WeeklyScheduleActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String[] DAY_KEYS   = {"mon","tue","wed","thu","fri","sat","sun"};
    private static final String[] DAY_LABELS = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
    private static final int[]    TAB_IDS    = {R.id.tabMon,R.id.tabTue,R.id.tabWed,R.id.tabThu,R.id.tabFri,R.id.tabSat,R.id.tabSun};
    private static final int[]    SUM_IDS    = {R.id.sumMon,R.id.sumTue,R.id.sumWed,R.id.sumThu,R.id.sumFri,R.id.sumSat,R.id.sumSun};

    private static final int HOUR_START = 8;
    private static final int HOUR_END   = 21;

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

    /** Initializes the day selection tabs and their click listeners. */
    private void setupDayTabs() {
        for (int i = 0; i < TAB_IDS.length; i++) {
            final int dayIndex = i;
            LinearLayout tab = findViewById(TAB_IDS[i]);
            if (tab != null) {
                tab.setOnClickListener(v -> switchDay(dayIndex));
            }
        }
        switchDay(0);
    }

    /**
     * Handles switching the view to a different day of the week.
     * @param dayIndex Index of the day (0-6).
     */
    private void switchDay(int dayIndex) {
        selectedDay = dayIndex;

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

    /**
     * Dynamically builds the hour selection grid for the selected day.
     * Implementation of US 13 availability selection.
     */
    private void buildHourGrid() {
        if (layoutHourGrid == null) return;
        layoutHourGrid.removeAllViews();

        List<Integer> dayHours = schedule.get(selectedDay);
        int dp8 = dp(8);

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

                TextView tvHour = new TextView(this);
                tvHour.setText(String.valueOf(h <= 12 ? h : h - 12));
                tvHour.setTextSize(18);
                tvHour.setTextColor(selected ? Color.WHITE : Color.parseColor("#071A3D"));
                tvHour.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHour.setGravity(Gravity.CENTER);

                TextView tvAmPm = new TextView(this);
                tvAmPm.setText(h < 12 ? "AM" : "PM");
                tvAmPm.setTextSize(11);
                tvAmPm.setTextColor(selected ? Color.parseColor("#CCFFFFFF") : Color.parseColor("#8B97A8"));
                tvAmPm.setGravity(Gravity.CENTER);

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

    private void updateSummary() {
        for (int i = 0; i < SUM_IDS.length; i++) {
            TextView tv = findViewById(SUM_IDS[i]);
            if (tv != null) tv.setText(schedule.get(i).size() + "h");
        }
    }

    /** Utility to replicate the current day's schedule across the entire week. */
    private void copyToAllDays() {
        List<Integer> source = new ArrayList<>(schedule.get(selectedDay));
        for (int i = 0; i < 7; i++) {
            schedule.get(i).clear();
            schedule.get(i).addAll(source);
        }
        updateSummary();
        Toast.makeText(this, "✅ Copied to all days!", Toast.LENGTH_SHORT).show();
    }

    /** Persists the entire weekly schedule to the Firestore availability collection. */
    private void saveSchedule() {
        if (currentUser == null) return;

        // Build a proper nested map — set() with dot-keyed strings creates literal field names
        // (e.g. "schedule.mon"), not nested paths. Only update() resolves dots to nested maps.
        Map<String, Object> dayMap = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            dayMap.put(DAY_KEYS[i], new ArrayList<>(schedule.get(i)));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("schedule", dayMap);

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "✅ Schedule saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Loads the saved schedule from Firestore on initialization. */
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
                        // Read from nested map structure (matches how saveSchedule() writes it)
                        Map<String, Object> scheduleMap = (Map<String, Object>) doc.get("schedule");
                        if (scheduleMap != null) {
                            for (int i = 0; i < 7; i++) {
                                Object raw = scheduleMap.get(DAY_KEYS[i]);
                                if (raw instanceof List) {
                                    List<?> saved = (List<?>) raw;
                                    schedule.get(i).clear();
                                    for (Object h : saved) {
                                        if (h instanceof Long) schedule.get(i).add(((Long) h).intValue());
                                        else if (h instanceof Integer) schedule.get(i).add((Integer) h);
                                    }
                                }
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