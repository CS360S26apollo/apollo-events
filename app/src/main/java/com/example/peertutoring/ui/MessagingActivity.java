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
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessagingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;
    private String convId;
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
        if (user == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUid      = user.getUid();
        currentUserName = getIntent().getStringExtra("currentUserName");
        if (currentUserName == null || currentUserName.isEmpty())
            currentUserName = user.getDisplayName() != null ? user.getDisplayName() : "Me";

        otherPersonName = getIntent().getStringExtra("otherPersonName");

        // --- Build convId ---
        // Priority 1: explicit tutorUid + studentUid passed
        String tutorUid   = getIntent().getStringExtra("tutorUid");
        String studentUid = getIntent().getStringExtra("studentUid");
        // Priority 2: otherUid passed directly
        String otherUid   = getIntent().getStringExtra("otherUid");
        // Priority 3: requestId used as convId (from TutorDetailActivity)
        String requestId  = getIntent().getStringExtra("requestId");

        if (tutorUid != null && studentUid != null) {
            convId = buildConvId(studentUid, tutorUid);
        } else if (otherUid != null) {
            convId = buildConvId(currentUid, otherUid);
        } else if (requestId != null && requestId.contains("_") && !requestId.startsWith("mock")) {
            convId = requestId; // already a stable convId
        } else {
            Toast.makeText(this, "Cannot open chat: missing participants.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Bind views
        layoutMessages = findViewById(R.id.layoutMessages);
        scrollMessages = findViewById(R.id.scrollMessages);
        etMessage      = findViewById(R.id.etMessage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        if (tvTitle != null && otherPersonName != null)
            tvTitle.setText(otherPersonName);

        View btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        // Also allow sending by keyboard action
        if (etMessage != null) {
            etMessage.setOnEditorActionListener((v, actionId, event) -> {
                sendMessage();
                return true;
            });
        }

        ensureConversationDoc();
        startListening();
    }

    // ── Stable conversation ID ────────────────────────────────

    private String buildConvId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0
                ? uid1 + "_" + uid2
                : uid2 + "_" + uid1;
    }

    private void ensureConversationDoc() {
        Map<String, Object> meta = new HashMap<>();
        String[] parts = convId.split("_", 2);
        meta.put("participantA",    parts.length > 0 ? parts[0] : currentUid);
        meta.put("participantB",    parts.length > 1 ? parts[1] : "");
        meta.put("otherPersonName", otherPersonName != null ? otherPersonName : "");
        meta.put("updatedAt",       FieldValue.serverTimestamp());
        db.collection("conversations").document(convId).set(meta, SetOptions.merge());
    }

    // ── Send ──────────────────────────────────────────────────

    private void sendMessage() {
        if (etMessage == null || currentUid.isEmpty()) return;
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

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
                    Toast.makeText(this, "Send failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    if (etMessage != null) {
                        etMessage.setText(text);
                        etMessage.setSelection(text.length());
                    }
                });

        // Update conversation last message
        Map<String, Object> last = new HashMap<>();
        last.put("lastMessage",   text);
        last.put("lastMessageAt", FieldValue.serverTimestamp());
        last.put("lastSenderUid", currentUid);
        db.collection("conversations").document(convId).set(last, SetOptions.merge());
    }

    // ── Listen ────────────────────────────────────────────────

    private void startListening() {
        // No orderBy — avoids needing a Firestore composite index
        // Messages are sorted client-side by timestamp
        messagesListener = db.collection("conversations")
                .document(convId)
                .collection("messages")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        android.util.Log.e("CHAT", "Snapshot error: " + e.getMessage(), e);
                        Toast.makeText(this, "Chat error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null || layoutMessages == null) return;

                    // Sort by timestamp client-side (ascending)
                    java.util.List<DocumentSnapshot> docs = new java.util.ArrayList<>(snap.getDocuments());
                    docs.sort((a, b) -> {
                        com.google.firebase.Timestamp ta = a.getTimestamp("timestamp");
                        com.google.firebase.Timestamp tb = b.getTimestamp("timestamp");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return -1;
                        if (tb == null) return 1;
                        return ta.compareTo(tb);
                    });

                    layoutMessages.removeAllViews();
                    for (DocumentSnapshot doc : docs) {
                        String senderUid  = doc.getString("senderUid");
                        String senderName = doc.getString("senderName");
                        String msgText    = doc.getString("text");
                        Date   ts         = doc.getDate("timestamp");
                        boolean isMine    = currentUid.equals(senderUid);
                        addBubble(senderName, msgText, ts, isMine);
                    }
                    scrollToBottom();
                });
    }

    // ── Bubble ────────────────────────────────────────────────

    private void addBubble(String senderName, String text, Date ts, boolean isMine) {
        if (text == null || layoutMessages == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rp);
        row.setGravity(isMine ? Gravity.END : Gravity.START);

        // Sender name for received messages
        if (!isMine && senderName != null && !senderName.isEmpty()) {
            TextView tvName = new TextView(this);
            tvName.setText(senderName);
            tvName.setTextSize(11f);
            tvName.setTextColor(0xFF8B97A8);
            tvName.setPadding(dp(6), 0, 0, dp(2));
            row.addView(tvName);
        }

        // Bubble card
        MaterialCardView bubble = new MaterialCardView(this);
        int maxWidth = (int)(getResources().getDisplayMetrics().widthPixels * 0.72f);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubble.setLayoutParams(bp);
        bubble.setRadius(dp(18));
        bubble.setCardElevation(0);
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
        if (ts != null) {
            TextView tvTime = new TextView(this);
            tvTime.setText(TIME_FMT.format(ts));
            tvTime.setTextSize(10f);
            tvTime.setTextColor(0xFF8B97A8);
            tvTime.setPadding(dp(6), dp(2), dp(6), 0);
            row.addView(tvTime);
        }

        layoutMessages.addView(row);
    }

    private void scrollToBottom() {
        if (scrollMessages != null)
            scrollMessages.post(() -> scrollMessages.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}