package com.example.peertutoring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AvailabilityDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView tvWeeklyHours, tvActiveDays, tvBlockedCount, tvTokensPerHour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_dashboard);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvWeeklyHours    = findViewById(R.id.tvWeeklyHours);
        tvActiveDays     = findViewById(R.id.tvActiveDays);
        tvBlockedCount   = findViewById(R.id.tvBlockedCount);
        tvTokensPerHour  = findViewById(R.id.tvTokensPerHour);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (findViewById(R.id.cardWeeklySchedule) != null)
            findViewById(R.id.cardWeeklySchedule).setOnClickListener(v ->
                    startActivity(new Intent(this, WeeklyScheduleActivity.class)));

        if (findViewById(R.id.cardBlockedDates) != null)
            findViewById(R.id.cardBlockedDates).setOnClickListener(v ->
                    startActivity(new Intent(this, BlockedDatesActivity.class)));

        if (findViewById(R.id.cardBufferPricing) != null)
            findViewById(R.id.cardBufferPricing).setOnClickListener(v ->
                    startActivity(new Intent(this, BufferPricingActivity.class)));

        loadStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats(); // refresh after returning from sub-screens
    }

    @SuppressWarnings("unchecked")
    private void loadStats() {
        if (currentUser == null) return;

        db.collection("tutorAvailability")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    int totalHours = 0;
                    int activeDays = 0;
                    String[] days = {"mon","tue","wed","thu","fri","sat","sun"};
                    for (String day : days) {
                        List<Long> hours = (List<Long>) doc.get("schedule." + day);
                        if (hours != null && !hours.isEmpty()) {
                            totalHours += hours.size();
                            activeDays++;
                        }
                    }

                    if (tvWeeklyHours  != null) tvWeeklyHours.setText(String.valueOf(totalHours));
                    if (tvActiveDays   != null) tvActiveDays.setText(String.valueOf(activeDays));

                    List<String> blocked = (List<String>) doc.get("blockedDates");
                    if (tvBlockedCount != null)
                        tvBlockedCount.setText(String.valueOf(blocked != null ? blocked.size() : 0));

                    Long rate = doc.getLong("hourlyRate");
                    if (tvTokensPerHour != null)
                        tvTokensPerHour.setText(String.valueOf(rate != null ? rate : 100));
                });
    }
}