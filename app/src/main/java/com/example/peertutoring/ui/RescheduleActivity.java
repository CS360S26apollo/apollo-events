package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.ConflictChecker;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * US-15: Lets a student pick a new time slot from the tutor's availability.
 * No token deduction — rescheduling is always penalty-free.
 * Conflict-checks the new slot (excluding the session being rescheduled).
 */
public class RescheduleActivity extends AppCompatActivity {

    private LinearLayout layoutSlots;
    private TextView tvTutorName, tvCurrentTime, tvNoSlots;
    private Button btnConfirm;

    private String selectedSlotDay  = null;
    private int    selectedSlotHour = -1;
    private Button selectedSlotButton = null;

    private FirebaseFirestore db;
    private String requestId, tutorUid, tutorName, studentUid;
    private int    durationMinutes;

    private static final String[] DAY_KEYS  = {"mon","tue","wed","thu","fri","sat","sun"};
    private static final String[] DAY_NAMES = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reschedule);

        db = FirebaseFirestore.getInstance();

        requestId       = getIntent().getStringExtra("requestId");
        tutorUid        = getIntent().getStringExtra("tutorUid");
        tutorName       = getIntent().getStringExtra("tutorName");
        studentUid      = getIntent().getStringExtra("studentUid");
        durationMinutes = getIntent().getIntExtra("durationMinutes", 60);
        long currentDateMs = getIntent().getLongExtra("currentDateMs", 0L);

        tvTutorName   = findViewById(R.id.tvTutorName);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvNoSlots     = findViewById(R.id.tvNoSlots);
        layoutSlots   = findViewById(R.id.layoutSlots);
        btnConfirm    = findViewById(R.id.btnConfirmReschedule);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvTutorName != null && tutorName != null)
            tvTutorName.setText("with " + tutorName);

        if (tvCurrentTime != null && currentDateMs > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            tvCurrentTime.setText("Current: " + sdf.format(new Date(currentDateMs)));
        }

        if (btnConfirm != null)
            btnConfirm.setOnClickListener(v -> { SoundManager.playClick(this); confirmReschedule(); });

        loadTutorSlots();
    }

    private void loadTutorSlots() {
        if (tutorUid == null || tutorUid.isEmpty()) { showNoSlots(); return; }

        // Prefer tutorAvailability/{uid}.schedule (set by WeeklyScheduleActivity)
        db.collection("tutorAvailability").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        //noinspection unchecked
                        Map<String, Object> schedule = (Map<String, Object>) doc.get("schedule");
                        if (schedule != null && !schedule.isEmpty()) {
                            displaySlots(schedule);
                            return;
                        }
                    }
                    // Fall back to users/{uid}.availability (set during onboarding)
                    loadSlotsFromUsersCollection();
                })
                .addOnFailureListener(e -> loadSlotsFromUsersCollection());
    }

    private void loadSlotsFromUsersCollection() {
        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    //noinspection unchecked
                    Map<String, Object> avail = doc.exists()
                            ? (Map<String, Object>) doc.get("availability") : null;
                    if (avail != null && !avail.isEmpty()) displaySlots(avail);
                    else showNoSlots();
                })
                .addOnFailureListener(e -> showNoSlots());
    }

    private void displaySlots(Map<String, Object> avail) {
        if (layoutSlots == null) return;
        layoutSlots.removeAllViews();
        boolean hasAny = false;

        for (int d = 0; d < DAY_KEYS.length; d++) {
            String dayKey  = DAY_KEYS[d];
            String dayName = DAY_NAMES[d];
            Object hoursObj = avail.get(dayKey);
            if (hoursObj == null) continue;
            //noinspection unchecked
            List<Long> hours = hoursObj instanceof List ? (List<Long>) hoursObj : null;
            if (hours == null || hours.isEmpty()) continue;
            hasAny = true;

            TextView dayLabel = new TextView(this);
            dayLabel.setText(dayName);
            dayLabel.setTextColor(Color.parseColor("#071A3D"));
            dayLabel.setTextSize(14f);
            dayLabel.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, 0, 0, dpToPx(8));
            dayLabel.setLayoutParams(labelParams);
            layoutSlots.addView(dayLabel);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (Long hourLong : hours) {
                if (hourLong == null) continue;
                final int    slotHour = hourLong.intValue();
                final String slotDay  = dayKey;

                Button chip = new Button(this);
                chip.setText(formatHour(slotHour));
                chip.setTextSize(13f);
                chip.setAllCaps(false);
                chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3EEFF")));
                chip.setTextColor(Color.parseColor("#8A2EFF"));
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(40));
                chipParams.setMargins(0, 0, dpToPx(8), 0);
                chip.setLayoutParams(chipParams);
                chip.setPadding(dpToPx(14), 0, dpToPx(14), 0);

                chip.setOnClickListener(v -> {
                    if (selectedSlotButton != null) {
                        selectedSlotButton.setBackgroundTintList(
                                ColorStateList.valueOf(Color.parseColor("#F3EEFF")));
                        selectedSlotButton.setTextColor(Color.parseColor("#8A2EFF"));
                    }
                    chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8A2EFF")));
                    chip.setTextColor(Color.WHITE);
                    selectedSlotDay    = slotDay;
                    selectedSlotHour   = slotHour;
                    selectedSlotButton = chip;
                });

                row.addView(chip);
            }

            HorizontalScrollView scroll = new HorizontalScrollView(this);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scrollParams.setMargins(0, 0, 0, dpToPx(16));
            scroll.setLayoutParams(scrollParams);
            scroll.addView(row);
            layoutSlots.addView(scroll);
        }

        if (!hasAny) showNoSlots();
    }

    private void showNoSlots() {
        if (tvNoSlots != null) tvNoSlots.setVisibility(View.VISIBLE);
    }

    private void confirmReschedule() {
        if (selectedSlotDay == null || selectedSlotHour < 0) {
            Toast.makeText(this, "Please select a new time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        Date newDate = nextOccurrenceOf(selectedSlotDay, selectedSlotHour);
        setBtnState(false, "Checking conflicts...");

        ConflictChecker.checkConflict(db, tutorUid, studentUid, newDate, durationMinutes, requestId,
                (hasConflict, reason) -> {
                    if (hasConflict) {
                        SoundManager.playError(this);
                        Toast.makeText(this, "Cannot reschedule: " + reason, Toast.LENGTH_LONG).show();
                        setBtnState(true, "Confirm Reschedule");
                        return;
                    }
                    saveNewSchedule(newDate);
                });
    }

    private void saveNewSchedule(Date newDate) {
        setBtnState(false, "Rescheduling...");
        db.collection("sessionRequests").document(requestId)
                .update("scheduledDate", newDate)
                .addOnSuccessListener(unused -> {
                    SoundManager.playSuccess(this);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault());
                    Toast.makeText(this,
                            "Rescheduled to " + sdf.format(newDate), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setBtnState(true, "Confirm Reschedule");
                });
    }

    private void setBtnState(boolean enabled, String text) {
        if (btnConfirm != null) {
            btnConfirm.setEnabled(enabled);
            btnConfirm.setText(text);
        }
    }

    private Date nextOccurrenceOf(String dayKey, int hour) {
        int targetCalDay;
        switch (dayKey) {
            case "mon": targetCalDay = Calendar.MONDAY;    break;
            case "tue": targetCalDay = Calendar.TUESDAY;   break;
            case "wed": targetCalDay = Calendar.WEDNESDAY; break;
            case "thu": targetCalDay = Calendar.THURSDAY;  break;
            case "fri": targetCalDay = Calendar.FRIDAY;    break;
            case "sat": targetCalDay = Calendar.SATURDAY;  break;
            default:    targetCalDay = Calendar.SUNDAY;    break;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while (cal.get(Calendar.DAY_OF_WEEK) != targetCalDay) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (!cal.getTime().after(new Date())) cal.add(Calendar.WEEK_OF_YEAR, 1);
        return cal.getTime();
    }

    private String formatHour(int hour) {
        if (hour == 0)  return "12:00 AM";
        if (hour < 12)  return hour + ":00 AM";
        if (hour == 12) return "12:00 PM";
        return (hour - 12) + ":00 PM";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
