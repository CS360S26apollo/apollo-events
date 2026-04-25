package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.utils.SoundManager;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SessionNotesActivity extends AppCompatActivity {

    private EditText etTopics, etTakeaways, etActionItems;
    private Button btnSendNotes;
    private FirebaseFirestore db;

    private String requestId, tutorUid, studentUid, tutorName, studentName, subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_notes);

        db = FirebaseFirestore.getInstance();

        requestId   = getIntent().getStringExtra("requestId");
        tutorUid    = getIntent().getStringExtra("tutorUid");
        studentUid  = getIntent().getStringExtra("studentUid");
        tutorName   = getIntent().getStringExtra("tutorName");
        studentName = getIntent().getStringExtra("studentName");
        subject     = getIntent().getStringExtra("subject");

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvSubtitle = findViewById(R.id.tvSessionSubtitle);
        if (tvSubtitle != null && subject != null)
            tvSubtitle.setText("Session: " + subject + " with " + (studentName != null ? studentName : "Student"));

        etTopics      = findViewById(R.id.etTopics);
        etTakeaways   = findViewById(R.id.etTakeaways);
        etActionItems = findViewById(R.id.etActionItems);
        btnSendNotes  = findViewById(R.id.btnSendNotes);

        if (btnSendNotes != null)
            btnSendNotes.setOnClickListener(v -> { SoundManager.playClick(this); validateAndSend(); });
    }

    private void validateAndSend() {
        String topics      = etTopics      != null ? etTopics.getText().toString().trim()      : "";
        String takeaways   = etTakeaways   != null ? etTakeaways.getText().toString().trim()   : "";
        String actionItems = etActionItems != null ? etActionItems.getText().toString().trim() : "";

        if (TextUtils.isEmpty(topics) && TextUtils.isEmpty(takeaways)) {
            Toast.makeText(this, "Please fill in at least Topics Covered or Key Takeaways",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (tutorUid == null || studentUid == null) {
            Toast.makeText(this, "Session data missing, cannot send notes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSendNotes != null) {
            btnSendNotes.setEnabled(false);
            btnSendNotes.setText("Sending...");
        }

        String formattedMessage = buildNoteMessage(topics, takeaways, actionItems);
        String convId = buildConvId(tutorUid, studentUid);

        // Ensure conversation metadata exists
        Map<String, Object> convMeta = new HashMap<>();
        convMeta.put("participantA", tutorUid.compareTo(studentUid) < 0 ? tutorUid : studentUid);
        convMeta.put("participantB", tutorUid.compareTo(studentUid) < 0 ? studentUid : tutorUid);
        convMeta.put("tutorName", tutorName != null ? tutorName : "Tutor");

        db.collection("conversations").document(convId)
                .set(convMeta, SetOptions.merge())
                .addOnSuccessListener(unused -> sendMessageToChat(convId, formattedMessage));
    }

    private void sendMessageToChat(String convId, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderUid",  tutorUid);
        message.put("senderName", tutorName != null ? tutorName : "Tutor");
        message.put("text",       text);
        message.put("timestamp",  FieldValue.serverTimestamp());
        message.put("type",       "session_notes");

        db.collection("conversations").document(convId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> markNotesAdded())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void markNotesAdded() {
        if (requestId == null || requestId.startsWith("mock_")) {
            onSuccess();
            return;
        }
        db.collection("sessionRequests").document(requestId)
                .update("notesAdded", true)
                .addOnSuccessListener(u -> onSuccess())
                .addOnFailureListener(e -> onSuccess()); // non-critical, still succeed
    }

    private void onSuccess() {
        SoundManager.playSuccess(this);
        Toast.makeText(this, "Notes sent to student's chat!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void resetButton() {
        if (btnSendNotes != null) {
            btnSendNotes.setEnabled(true);
            btnSendNotes.setText("📤  Send Notes to Student");
        }
    }

    public static String buildNoteMessage(String topics, String takeaways, String actionItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("📝 SESSION NOTES\n");
        sb.append("─────────────────────────\n");
        if (!TextUtils.isEmpty(topics)) {
            sb.append("📚 Topics Covered:\n").append(topics).append("\n\n");
        }
        if (!TextUtils.isEmpty(takeaways)) {
            sb.append("💡 Key Takeaways:\n").append(takeaways).append("\n\n");
        }
        if (!TextUtils.isEmpty(actionItems)) {
            sb.append("✅ Action Items:\n").append(actionItems).append("\n\n");
        }
        sb.append("─────────────────────────");
        return sb.toString().trim();
    }

    public static String buildConvId(String tutorUid, String studentUid) {
        return tutorUid.compareTo(studentUid) < 0
                ? tutorUid + "_" + studentUid
                : studentUid + "_" + tutorUid;
    }
}
