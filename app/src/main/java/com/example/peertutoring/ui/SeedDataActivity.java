package com.example.peertutoring.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.Tutor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedDataActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvRequestsStatus;
    private Button btnSeedTutors;
    private Button btnSeedRequests;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seed_data);

        db = FirebaseFirestore.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvStatus         = findViewById(R.id.tvSeedStatus);
        tvRequestsStatus = findViewById(R.id.tvSeedRequestsStatus);
        btnSeedTutors    = findViewById(R.id.btnSeedTutors);
        btnSeedRequests  = findViewById(R.id.btnSeedRequests);

        if (btnSeedTutors != null)
            btnSeedTutors.setOnClickListener(v -> seedTutors());

        if (btnSeedRequests != null)
            btnSeedRequests.setOnClickListener(v -> seedRequests());
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

    // ── Seed session requests targeting the logged-in tutor ──────────────────

    private void seedRequests() {
        String tutorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (tutorUid.isEmpty()) {
            setRequestsStatus("⚠️ Not logged in. Sign in as a tutor first.");
            return;
        }

        if (btnSeedRequests != null) {
            btnSeedRequests.setEnabled(false);
            btnSeedRequests.setText("Seeding...");
        }
        setRequestsStatus("Fetching your tutor profile...");

        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        setRequestsStatus("⚠️ Tutor profile not found. Complete your profile first.");
                        resetRequestsButton();
                        return;
                    }
                    String tutorName = doc.getString("fullName");
                    if (tutorName == null) tutorName = "Tutor";

                    @SuppressWarnings("unchecked")
                    List<String> subjects = (List<String>) doc.get("subjects");
                    if (subjects == null || subjects.isEmpty()) {
                        subjects = Arrays.asList("Mathematics", "Computer Science");
                    }

                    Long rateLong = doc.getLong("rate");
                    int rate = rateLong != null ? rateLong.intValue() : 40;

                    writeStudentsAndRequests(tutorUid, tutorName, subjects, rate);
                })
                .addOnFailureListener(e -> {
                    setRequestsStatus("⚠️ Error: " + e.getMessage());
                    resetRequestsButton();
                });
    }

    private void writeStudentsAndRequests(String tutorUid, String tutorName,
                                          List<String> subjects, int rate) {
        // Three fake student docs — fixed IDs so re-runs just overwrite them
        String[][] students = {
                {"seed_student_sara",  "Sara Ahmed",   "sara.ahmed@test.lums.edu.pk"},
                {"seed_student_hamza", "Hamza Raza",   "hamza.raza@test.lums.edu.pk"},
                {"seed_student_nadia", "Nadia Farooq", "nadia.farooq@test.lums.edu.pk"},
        };

        AtomicInteger done = new AtomicInteger(0);
        int total = students.length;

        for (String[] s : students) {
            Map<String, Object> studentDoc = new HashMap<>();
            studentDoc.put("uid",      s[0]);
            studentDoc.put("role",     "student");
            studentDoc.put("fullName", s[1]);
            studentDoc.put("email",    s[2]);
            studentDoc.put("tokens",   150L);

            db.collection("users").document(s[0]).set(studentDoc)
                    .addOnCompleteListener(task -> {
                        if (done.incrementAndGet() == total) {
                            createSessionRequests(tutorUid, tutorName, subjects, rate, students);
                        }
                    });
        }
    }

    private void createSessionRequests(String tutorUid, String tutorName,
                                       List<String> subjects, int rate,
                                       String[][] students) {
        // Topic suggestions keyed by subject (lowercase)
        Map<String, String[]> topicMap = buildTopicMap();

        Calendar cal = Calendar.getInstance();

        AtomicInteger done    = new AtomicInteger(0);
        AtomicInteger failed  = new AtomicInteger(0);
        int total = students.length;

        for (int i = 0; i < students.length; i++) {
            String subject = subjects.get(i % subjects.size());
            String[] topics = topicMap.containsKey(subject.toLowerCase())
                    ? topicMap.get(subject.toLowerCase())
                    : new String[]{"General concepts", "Problem solving", "Exam preparation"};
            String topic = topics[i % topics.length];

            int durationMinutes = (i == 1) ? 90 : 60;
            int tokens = (int) Math.ceil(durationMinutes / 60.0) * rate;

            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, i + 2);
            cal.set(Calendar.HOUR_OF_DAY, 10 + (i * 4));
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date scheduledDate = cal.getTime();

            String sessionDate = String.format(Locale.getDefault(), "%tB %td, %tY", cal, cal, cal);
            String sessionTime = String.format(Locale.getDefault(), "%02d:00", 10 + (i * 4));

            Map<String, Object> request = new HashMap<>();
            request.put("tutorUid",       tutorUid);
            request.put("tutorName",      tutorName);
            request.put("studentUid",     students[i][0]);
            request.put("studentName",    students[i][1]);
            request.put("subject",        subject);
            request.put("topic",          topic);
            request.put("goals",          GOALS[i % GOALS.length]);
            request.put("studentMessage", MESSAGES[i % MESSAGES.length]);
            request.put("durationMinutes", (long) durationMinutes);
            request.put("tokens",          (long) tokens);
            request.put("status",          "requested");
            request.put("scheduledDate",   scheduledDate);
            request.put("sessionDate",     sessionDate);
            request.put("sessionTime",     sessionTime);
            request.put("createdAt",       FieldValue.serverTimestamp());

            db.collection("sessionRequests").add(request)
                    .addOnSuccessListener(ref -> {
                        int n = done.incrementAndGet();
                        if (n + failed.get() == total) onRequestsSeedComplete(n, failed.get());
                    })
                    .addOnFailureListener(e -> {
                        int f = failed.incrementAndGet();
                        if (done.get() + f == total) onRequestsSeedComplete(done.get(), f);
                    });
        }
    }

    private void onRequestsSeedComplete(int succeeded, int failed) {
        resetRequestsButton();
        if (failed == 0) {
            setRequestsStatus("✅ " + succeeded
                    + " test requests created! Open Requests in your tutor dashboard.");
        } else {
            setRequestsStatus("⚠️ " + succeeded + " succeeded, " + failed
                    + " failed. Check Firestore rules.");
        }
    }

    private void resetRequestsButton() {
        if (btnSeedRequests != null) {
            btnSeedRequests.setEnabled(true);
            btnSeedRequests.setText("Seed Test Requests");
        }
    }

    private void setRequestsStatus(String msg) {
        if (tvRequestsStatus != null) {
            tvRequestsStatus.setVisibility(View.VISIBLE);
            tvRequestsStatus.setText(msg);
        }
    }

    private Map<String, String[]> buildTopicMap() {
        Map<String, String[]> m = new HashMap<>();
        m.put("mathematics",      new String[]{"Calculus II — Integration by Parts", "Linear Algebra — Eigenvalues", "Probability & Statistics"});
        m.put("physics",          new String[]{"Quantum Mechanics — Wave Functions",  "Thermodynamics — Entropy",    "Classical Mechanics"});
        m.put("computer science", new String[]{"Data Structures — Binary Trees",      "Algorithm Analysis — Big O",  "Operating Systems"});
        m.put("chemistry",        new String[]{"Organic Chemistry — SN1 vs SN2",      "Thermochemistry",             "Chemical Equilibrium"});
        m.put("biology",          new String[]{"Cell Biology — Mitosis",              "Genetics — DNA Replication",  "Ecology"});
        m.put("economics",        new String[]{"Microeconomics — Supply & Demand",    "Macroeconomics — GDP",        "Game Theory"});
        m.put("english",          new String[]{"Essay Writing Techniques",            "Grammar & Syntax",            "Literature Analysis"});
        m.put("history",          new String[]{"World War II Analysis",               "Colonial History",            "Modern Political History"});
        return m;
    }

    private static final String[] GOALS = {
            "Exam Prep",
            "Concept Review",
            "Homework Help",
    };

    private static final String[] MESSAGES = {
            "Hi! I have an exam next week and need help solidifying my understanding of the core concepts.",
            "I've been struggling with this topic for a while. Need a clear explanation and practice problems.",
            "Looking for help with my assignment. Specifically confused about a few key ideas.",
    };
}
