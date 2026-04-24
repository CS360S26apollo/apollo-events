package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Real-time messaging between a student and tutor.
 *
 * Messages stored under: conversations/{convId}/messages/{msgId}
 * Each message: { senderUid, senderName, text, timestamp }
 *
 * Crash fixes:
 * - currentUserName now resolved from Firestore profile (not just DisplayName which is often null)
 * - All findViewById results null-checked
 * - requestId validated before attaching listener
 * - ScrollView cast done safely
 */
public class MessagingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid = "";
    private String currentUserName = "Me";
    private String conversationId;
    private String otherPersonName;

    private LinearLayout layoutMessages;
    private ScrollView scrollMessages;
    private EditText etMessage;
    private ListenerRegistration messagesListener;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        db = FirebaseFirestore.getInstance();

        conversationId  = getIntent().getStringExtra("requestId");
        otherPersonName = getIntent().getStringExtra("otherPersonName");
        // currentUserName from intent is a hint; we'll resolve from Firestore below
        String nameHint = getIntent().getStringExtra("currentUserName");
        if (nameHint != null && !nameHint.isEmpty()) currentUserName = nameHint;

        layoutMessages = findViewById(R.id.layoutMessages);
        scrollMessages = findViewById(R.id.scrollMessages);
        etMessage      = findViewById(R.id.etMessage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        if (tvTitle != null) tvTitle.setText(otherPersonName != null ? otherPersonName : "Chat");

        View btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        // Resolve current user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            resolveUserNameThenListen();
        } else {
            Toast.makeText(this, "Please sign in to use messaging.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Fetches the user's actual name from their Firestore profile,
     * then starts the real-time message listener.
     * Firebase DisplayName is often empty for email/password users.
     */
    private void resolveUserNameThenListen() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String firstName = doc.getString("firstName");
                        if (fullName != null && !fullName.isEmpty()) {
                            currentUserName = fullName;
                        } else if (firstName != null && !firstName.isEmpty()) {
                            currentUserName = firstName;
                        }
                    }
                    startListening();
                })
                .addOnFailureListener(e -> startListening()); // proceed anyway
    }

    /**
     * Sends a message to the Firestore messages sub-collection under the conversation.
     */
    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (conversationId == null || conversationId.isEmpty()) {
            Toast.makeText(this, "Cannot send message — invalid conversation.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderUid",  currentUid);
        msg.put("senderName", currentUserName);
        msg.put("text",       text);
        msg.put("timestamp",  FieldValue.serverTimestamp());

        // Store under conversations/{convId}/messages
        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Attaches a real-time listener for messages ordered by timestamp ascending.
     */
    private void startListening() {
        if (conversationId == null || conversationId.isEmpty()) return;

        messagesListener = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || layoutMessages == null) return;
                    layoutMessages.removeAllViews();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String senderUid  = doc.getString("senderUid");
                        String senderName = doc.getString("senderName");
                        String text       = doc.getString("text");
                        Date   ts         = doc.getDate("timestamp");
                        boolean isMine    = currentUid.equals(senderUid);
                        addMessageBubble(senderName, text, ts, isMine);
                    }
                    scrollToBottom();
                });
    }

    /**
     * Dynamically builds and adds a message bubble to the chat view.
     */
    private void addMessageBubble(String senderName, String text, Date timestamp, boolean isMine) {
        if (text == null || text.isEmpty() || layoutMessages == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);
        row.setGravity(isMine ? Gravity.END : Gravity.START);

        // Sender name label (only for received messages)
        if (!isMine && senderName != null && !senderName.isEmpty()) {
            TextView tvName = new TextView(this);
            tvName.setText(senderName);
            tvName.setTextSize(11);
            tvName.setTextColor(0xFF8B97A8);
            tvName.setPadding(dp(4), 0, 0, dp(2));
            row.addView(tvName);
        }

        // Bubble
        MaterialCardView bubble = new MaterialCardView(this);
        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.72);
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bubble.setLayoutParams(bubbleParams);
        bubble.setCardElevation(0);
        bubble.setRadius(dp(16));
        bubble.setCardBackgroundColor(isMine ? 0xFF8A2EFF : 0xFFFFFFFF);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(isMine ? 0xFFFFFFFF : 0xFF071A3D);
        tvText.setTextSize(15);
        tvText.setPadding(dp(14), dp(10), dp(14), dp(10));
        tvText.setMaxWidth(maxWidth);
        bubble.addView(tvText);
        row.addView(bubble);

        // Timestamp
        if (timestamp != null) {
            TextView tvTime = new TextView(this);
            tvTime.setText(TIME_FMT.format(timestamp));
            tvTime.setTextSize(10);
            tvTime.setTextColor(0xFF8B97A8);
            tvTime.setPadding(dp(4), dp(2), dp(4), 0);
            row.addView(tvTime);
        }

        layoutMessages.addView(row);
    }

    private void scrollToBottom() {
        if (scrollMessages != null) {
            scrollMessages.post(() -> scrollMessages.fullScroll(View.FOCUS_DOWN));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}