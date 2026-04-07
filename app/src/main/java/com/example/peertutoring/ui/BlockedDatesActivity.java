package com.example.peertutoring.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for tutors to manage specific calendar dates where they are unavailable.
 * Role: Calendar Management View for User Story 13 (Tutor Availability).
 * Purpose: Allows tutors to block off entire days (e.g., for holidays or personal breaks)
 * by interacting with a custom-rendered calendar interface.
 * 
 * Design Pattern: Custom View Rendering / State Management.
 * 
 * Outstanding Issues:
 * - Does not currently support recurring blocked dates (e.g., every Monday).
 * - Performance may degrade if hundreds of dates are blocked over multiple years.
 */
public class BlockedDatesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private LinearLayout layoutCalendarGrid, layoutRecentDates;
    private TextView tvMonthYear, tvBlockedCount;
    private final Calendar displayedMonth = Calendar.getInstance();
    private final List<String> blockedDates = new ArrayList<>(); // "yyyy-MM-dd"

    private static final String[] MONTHS = {
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
    };
    private static final String[] DAY_NAMES = {
            "Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_dates);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        layoutCalendarGrid = findViewById(R.id.layoutCalendarGrid);
        layoutRecentDates  = findViewById(R.id.layoutRecentDates);
        tvMonthYear        = findViewById(R.id.tvMonthYear);
        tvBlockedCount     = findViewById(R.id.tvBlockedCount);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        MaterialCardView btnPrev = findViewById(R.id.btnPrevMonth);
        MaterialCardView btnNext = findViewById(R.id.btnNextMonth);
        if (btnPrev != null) btnPrev.setOnClickListener(v -> { changeMonth(-1); });
        if (btnNext != null) btnNext.setOnClickListener(v -> { changeMonth(1); });

        if (findViewById(R.id.btnClearAll) != null)
            findViewById(R.id.btnClearAll).setOnClickListener(v -> clearAll());

        loadBlockedDates();
    }

    /**
     * Changes the currently displayed month in the calendar.
     * @param delta Number of months to add (positive) or subtract (negative).
     */
    private void changeMonth(int delta) {
        displayedMonth.add(Calendar.MONTH, delta);
        buildCalendar();
    }

    /**
     * Dynamically constructs the calendar grid for the selected month.
     * Logic maps the 1st of the month to its correct Day of Week.
     */
    private void buildCalendar() {
        if (layoutCalendarGrid == null) return;
        layoutCalendarGrid.removeAllViews();

        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH);

        if (tvMonthYear != null)
            tvMonthYear.setText(MONTHS[month] + " " + year);

        Calendar first = Calendar.getInstance();
        first.set(year, month, 1);
        int startDow = first.get(Calendar.DAY_OF_WEEK); // 1=Sun
        int offset = (startDow - 2 + 7) % 7;

        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        int todayYear  = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay   = today.get(Calendar.DAY_OF_MONTH);

        int dp6 = dp(6);
        int cellSize = dp(44);

        int day = 1;
        while (day <= daysInMonth) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, dp6);
            row.setLayoutParams(rp);

            for (int col = 0; col < 7; col++) {
                boolean isEmpty = (day == 1 && col < offset) || day > daysInMonth;
                final int thisDay;
                if (isEmpty) {
                    thisDay = -1;
                } else {
                    thisDay = day;
                    if (day <= daysInMonth) day++;
                }

                MaterialCardView cell = new MaterialCardView(this);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, cellSize, 1f);
                cp.setMargins(col > 0 ? dp(4) : 0, 0, 0, 0);
                cell.setLayoutParams(cp);
                cell.setRadius(dp(10));
                cell.setCardElevation(0);

                if (thisDay == -1) {
                    cell.setCardBackgroundColor(Color.TRANSPARENT);
                    cell.setCardElevation(0);
                } else {
                    String dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, thisDay);
                    boolean isBlocked = blockedDates.contains(dateKey);
                    boolean isToday   = year == todayYear && month == todayMonth && thisDay == todayDay;

                    if (isBlocked) {
                        cell.setCardBackgroundColor(Color.parseColor("#F44336"));
                    } else if (isToday) {
                        cell.setCardBackgroundColor(Color.parseColor("#BBDEFB"));
                    } else {
                        cell.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
                    }

                    TextView tv = new TextView(this);
                    tv.setText(String.valueOf(thisDay));
                    tv.setTextSize(14);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextColor(isBlocked ? Color.WHITE : Color.parseColor("#071A3D"));
                    tv.setTypeface(null, isToday ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                    tv.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
                    cell.addView(tv);

                    final String dk = dateKey;
                    cell.setOnClickListener(v -> toggleDate(dk));
                }

                row.addView(cell);
            }
            layoutCalendarGrid.addView(row);
        }
    }

    /**
     * Toggles the blocked status of a specific date and persists the change.
     * @param dateKey The date string in "yyyy-MM-dd" format.
     */
    private void toggleDate(String dateKey) {
        if (blockedDates.contains(dateKey)) {
            blockedDates.remove(dateKey);
        } else {
            blockedDates.add(dateKey);
            Toast.makeText(this, "Date blocked!", Toast.LENGTH_SHORT).show();
        }
        updateBlockedCount();
        buildCalendar();
        buildRecentList();
        saveBlockedDates();
    }

    /**
     * Removes all blocked dates from the tutor's schedule.
     */
    private void clearAll() {
        blockedDates.clear();
        updateBlockedCount();
        buildCalendar();
        buildRecentList();
        saveBlockedDates();
        Toast.makeText(this, "All blocked dates cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Generates a list of recently blocked dates for quick reference below the calendar.
     */
    private void buildRecentList() {
        if (layoutRecentDates == null) return;
        layoutRecentDates.removeAllViews();

        if (blockedDates.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No blocked dates yet");
            empty.setTextColor(Color.parseColor("#8B97A8"));
            empty.setTextSize(14);
            layoutRecentDates.addView(empty);
            return;
        }

        List<String> recent = new ArrayList<>(blockedDates);
        java.util.Collections.sort(recent, java.util.Collections.reverseOrder());
        int limit = Math.min(5, recent.size());

        for (int i = 0; i < limit; i++) {
            String dk = recent.get(i);
            String[] parts = dk.split("-");
            if (parts.length < 3) continue;

            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day   = Integer.parseInt(parts[2]);

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            String dayName = DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1];
            String display = MONTHS[month].substring(0, 3) + " " + day + ", " + year;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(rp);

            MaterialCardView icon = new MaterialCardView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(40), dp(40));
            ip.setMargins(0, 0, dp(12), 0);
            icon.setLayoutParams(ip);
            icon.setRadius(dp(12));
            icon.setCardBackgroundColor(Color.parseColor("#EEF2FF"));
            icon.setCardElevation(0);
            TextView tvIcon = new TextView(this);
            tvIcon.setText("📅");
            tvIcon.setTextSize(16);
            tvIcon.setGravity(android.view.Gravity.CENTER);
            tvIcon.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            icon.addView(tvIcon);
            row.addView(icon);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDay = new TextView(this);
            tvDay.setText(dayName);
            tvDay.setTextColor(Color.parseColor("#071A3D"));
            tvDay.setTextSize(14);
            tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(tvDay);

            TextView tvDate = new TextView(this);
            tvDate.setText(display);
            tvDate.setTextColor(Color.parseColor("#8B97A8"));
            tvDate.setTextSize(12);
            textCol.addView(tvDate);

            row.addView(textCol);

            MaterialCardView del = new MaterialCardView(this);
            LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(36), dp(36));
            del.setLayoutParams(dp2);
            del.setRadius(dp(10));
            del.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            del.setCardElevation(0);
            TextView tvDel = new TextView(this);
            tvDel.setText("🗑");
            tvDel.setTextSize(14);
            tvDel.setGravity(android.view.Gravity.CENTER);
            tvDel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            del.addView(tvDel);

            final String toDelete = dk;
            del.setOnClickListener(v -> toggleDate(toDelete));
            row.addView(del);

            layoutRecentDates.addView(row);
        }
    }

    private void updateBlockedCount() {
        if (tvBlockedCount != null) tvBlockedCount.setText(String.valueOf(blockedDates.size()));
    }

    /** Saves the current list of blocked dates to the Firestore availability collection. */
    private void saveBlockedDates() {
        if (currentUser == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("blockedDates", new ArrayList<>(blockedDates));
        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .set(data, SetOptions.merge());
    }

    /** Fetches the tutor's blocked dates from Firestore upon initialization. */
    @SuppressWarnings("unchecked")
    private void loadBlockedDates() {
        if (currentUser == null) {
            buildCalendar();
            buildRecentList();
            return;
        }

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> saved = (List<String>) doc.get("blockedDates");
                        if (saved != null) {
                            blockedDates.clear();
                            blockedDates.addAll(saved);
                        }
                    }
                    updateBlockedCount();
                    buildCalendar();
                    buildRecentList();
                })
                .addOnFailureListener(e -> {
                    buildCalendar();
                    buildRecentList();
                });
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}