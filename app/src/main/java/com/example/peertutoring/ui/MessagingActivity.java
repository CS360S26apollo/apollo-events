package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Real-time chat between student and tutor.
 *
 * Storage path: conversations/{convId}/messages/{msgId}
 * convId is always built by sorting both UIDs alphabetically so both
 * users always land on the SAME thread regardless of who opens it.
 *
 * Both participants see all messages in real time via snapshot listener.
 * Sent = purple bubble on right. Received = white bubble on left.
 */
public class MessagingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;
    private String convId;          // stable conversation ID
    private String otherPersonName;

    private LinearLayout layoutMessages;
    private ScrollView   scrollMessages;
    private EditText     etMessage;
    private ListenerRegistration messagesListener;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid      = user != null ? user.getUid() : "";
        currentUserName = getIntent().getStringExtra("currentUserName");
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = user != null && user.getDisplayName() != null
                    ? user.getDisplayName() : "Me";
        }

        // requestId is used as convId when opened from TutorDetailActivity
        // (it already contains the sorted UID pair e.g. "uid_aaa_uid_bbb")
        String intentConvId = getIntent().getStringExtra("requestId");
        String tutorUid     = getIntent().getStringExtra("tutorUid");
        String studentUid   = getIntent().getStringExtra("studentUid");
        otherPersonName     = getIntent().getStringExtra("otherPersonName");

        // Build stable convId: sort current + other UID alphabetically
        if (intentConvId != null && intentConvId.contains("_")
                && !intentConvId.startsWith("mock")) {
            // Already a convId (from TutorDetailActivity or SessionNotesActivity)
            convId = intentConvId;
        } else if (tutorUid != null && studentUid != null) {
            convId = buildConvId(studentUid, tutorUid);
        } else {
            // Derive other uid from intent if available
            String otherUid = getIntent().getStringExtra("otherUid");
            if (otherUid != null && !currentUid.isEmpty()) {
                convId = buildConvId(currentUid, otherUid);
            } else {
                convId = intentConvId; // fallback
            }
        }

        layoutMessages = findViewById(R.id.layoutMessages);
        scrollMessages = findViewById(R.id.scrollMessages);
        etMessage      = findViewById(R.id.etMessage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        if (tvTitle != null && otherPersonName != null) tvTitle.setText(otherPersonName);

        View btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        if (convId != null && !convId.isEmpty()) {
            ensureConversationExists();
            startListening();
        } else {
            Toast.makeText(this, "Cannot open chat: missing conversation ID.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Stable conversation ID ────────────────────────────────

    /** Sorts two UIDs so the ID is the same regardless of who opens the chat. */
    private String buildConvId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0
                ? uid1 + "_" + uid2
                : uid2 + "_" + uid1;
    }

    /** Creates conversation metadata in Firestore if it doesn't exist yet. */
    private void ensureConversationExists() {
        Map<String, Object> meta = new HashMap<>();
        // Store participant UIDs so either user can query their conversations
        String[] parts = convId.split("_", 2);
        meta.put("participantA",    parts.length > 0 ? parts[0] : currentUid);
        meta.put("participantB",    parts.length > 1 ? parts[1] : "");
        meta.put("otherPersonName", otherPersonName != null ? otherPersonName : "");
        meta.put("updatedAt",       FieldValue.serverTimestamp());

        db.collection("conversations").document(convId)
                .set(meta, SetOptions.merge());
    }

    // ── Send ──────────────────────────────────────────────────

    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || currentUid.isEmpty() || convId == null) return;

        // Optimistic clear
        etMessage.setText("");

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderUid",  currentUid);
        msg.put("senderName", currentUserName);
        msg.put("text",       text);
        msg.put("timestamp",  FieldValue.serverTimestamp());

        db.collection("conversations").document(convId)
                .collection("messages")
                .add(msg)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Restore text on failure
                    etMessage.setText(text);
                    etMessage.setSelection(text.length());
                });

        // Update conversation last message for listing purposes
        Map<String, Object> lastMsg = new HashMap<>();
        lastMsg.put("lastMessage", text);
        lastMsg.put("lastMessageAt", FieldValue.serverTimestamp());
        lastMsg.put("lastSenderUid", currentUid);
        db.collection("conversations").document(convId)
                .set(lastMsg, SetOptions.merge());
    }

    // ── Listen ────────────────────────────────────────────────

    private void startListening() {
        messagesListener = db.collection("conversations")
                .document(convId)
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

    // ── Message bubble ────────────────────────────────────────

    private void addMessageBubble(String senderName, String text, Date timestamp, boolean isMine) {
        if (layoutMessages == null || text == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);
        row.setGravity(isMine ? Gravity.END : Gravity.START);

        // Sender name (only for received)
        if (!isMine && senderName != null && !senderName.isEmpty()) {
            TextView tvName = new TextView(this);
            tvName.setText(senderName);
            tvName.setTextSize(11f);
            tvName.setTextColor(0xFF8B97A8);
            tvName.setPadding(dp(6), 0, 0, dp(2));
            row.addView(tvName);
        }

        // Bubble
        MaterialCardView bubble = new MaterialCardView(this);
        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.72f);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubble.setLayoutParams(bp);
        bubble.setCardElevation(0);
        bubble.setMaxCardElevation(0);
        bubble.setRadius(dp(18));
        bubble.setCardBackgroundColor(isMine ? 0xFF8A2EFF : 0xFFFFFFFF);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(isMine ? 0xFFFFFFFF : 0xFF071A3D);
        tvText.setTextSize(15f);
        tvText.setPadding(dp(14), dp(10), dp(14), dp(10));
        tvText.setMaxWidth(maxWidth);
        bubble.addView(tvText);
        row.addView(bubble);

        // Timestamp
        if (timestamp != null) {
            TextView tvTime = new TextView(this);
            tvTime.setText(TIME_FMT.format(timestamp));
            tvTime.setTextSize(10f);
            tvTime.setTextColor(0xFF8B97A8);
            tvTime.setPadding(dp(6), dp(2), dp(6), 0);
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