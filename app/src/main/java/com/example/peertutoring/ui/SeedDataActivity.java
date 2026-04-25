package com.example.peertutoring.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.Tutor;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedDataActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnSeedTutors;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seed_data);

        db = FirebaseFirestore.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvStatus       = findViewById(R.id.tvSeedStatus);
        btnSeedTutors  = findViewById(R.id.btnSeedTutors);

        if (btnSeedTutors != null)
            btnSeedTutors.setOnClickListener(v -> seedTutors());
    }

    private void seedTutors() {
        btnSeedTutors.setEnabled(false);
        btnSeedTutors.setText("Seeding...");
        setStatus("Writing tutor documents...");

        List<Tutor> tutors = buildTestTutors();
        AtomicInteger done = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int total = tutors.size();

        for (Tutor tutor : tutors) {
            db.collection("users").document(tutor.getUid())
                    .set(tutor)
                    .addOnSuccessListener(unused -> {
                        int n = done.incrementAndGet();
                        if (n + failed.get() == total) onSeedComplete(n, failed.get());
                    })
                    .addOnFailureListener(e -> {
                        int f = failed.incrementAndGet();
                        if (done.get() + f == total) onSeedComplete(done.get(), f);
                    });
        }
    }

    private void onSeedComplete(int succeeded, int failed) {
        btnSeedTutors.setEnabled(true);
        btnSeedTutors.setText("Seed Test Tutors");
        if (failed == 0) {
            setStatus("✅ " + succeeded + " tutors seeded! Open Browse Tutors to see them.");
        } else {
            setStatus("⚠️ " + succeeded + " succeeded, " + failed + " failed. Check Firestore rules.");
        }
    }

    private void setStatus(String msg) {
        if (tvStatus != null) {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(msg);
        }
    }

    private List<Tutor> buildTestTutors() {
        // ── Aisha Malik — Math & Physics, Mon/Tue/Thu slots ──
        Tutor aisha = new Tutor(
                "seed_tutor_aisha",
                "aisha.malik@apollo.test",
                "Aisha", "Malik",
                "Top-ranked Mathematics and Physics tutor at LUMS. 3 years teaching experience.",
                "Senior",
                40,
                Arrays.asList("Mathematics", "Physics")
        );
        aisha.setRating(4.8);
        aisha.setResponsivenessScore(0.95);
        aisha.setVerified(true);
        Map<String, List<Integer>> availAisha = new HashMap<>();
        availAisha.put("mon", Arrays.asList(9, 10, 14));
        availAisha.put("tue", Arrays.asList(10, 11));
        availAisha.put("thu", Arrays.asList(15, 16));
        aisha.setAvailability(availAisha);

        // ── Carlos Rivera — Chemistry & Biology, Wed/Fri slots ──
        Tutor carlos = new Tutor(
                "seed_tutor_carlos",
                "carlos.rivera@apollo.test",
                "Carlos", "Rivera",
                "Pre-med student specialising in Chemistry and Biology. Friendly and patient.",
                "Junior",
                35,
                Arrays.asList("Chemistry", "Biology")
        );
        carlos.setRating(4.5);
        carlos.setResponsivenessScore(0.85);
        carlos.setVerified(false);
        Map<String, List<Integer>> availCarlos = new HashMap<>();
        availCarlos.put("wed", Arrays.asList(10, 11, 14));
        availCarlos.put("fri", Arrays.asList(9, 10));
        carlos.setAvailability(availCarlos);

        // ── Priya Sharma — CS & Mathematics, Mon/Wed/Fri slots ──
        Tutor priya = new Tutor(
                "seed_tutor_priya",
                "priya.sharma@apollo.test",
                "Priya", "Sharma",
                "Computer Science graduate specialising in algorithms, data structures and calculus.",
                "Expert",
                50,
                Arrays.asList("Computer Science", "Mathematics")
        );
        priya.setRating(4.9);
        priya.setResponsivenessScore(0.98);
        priya.setVerified(true);
        Map<String, List<Integer>> availPriya = new HashMap<>();
        availPriya.put("mon", Arrays.asList(14, 15));
        availPriya.put("wed", Arrays.asList(9, 10));
        availPriya.put("fri", Arrays.asList(14, 15));
        priya.setAvailability(availPriya);

        // ── Omar Siddiqui — Economics & History, Tue/Thu/Sat slots ──
        Tutor omar = new Tutor(
                "seed_tutor_omar",
                "omar.siddiqui@apollo.test",
                "Omar", "Siddiqui",
                "Economics honours student. Makes complex theories easy to understand.",
                "Junior",
                30,
                Arrays.asList("Economics", "History")
        );
        omar.setRating(4.2);
        omar.setResponsivenessScore(0.80);
        omar.setVerified(false);
        Map<String, List<Integer>> availOmar = new HashMap<>();
        availOmar.put("tue", Arrays.asList(14, 15));
        availOmar.put("thu", Arrays.asList(10, 11));
        availOmar.put("sat", Arrays.asList(10, 11, 14));
        omar.setAvailability(availOmar);

        return Arrays.asList(aisha, carlos, priya, omar);
    }
}
