package com.example.peertutoring.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Real-time messaging activity between a student and tutor for a session request.
 *
 * Messages are stored in Firestore under:
 *   sessionRequests/{requestId}/messages/{messageId}
 *
 * Each message document has: senderUid, senderName, text, timestamp.
 * The UI uses real-time snapshot listeners so new messages appear instantly.
 */
public class MessagingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;
    private String requestId;
    private String otherPersonName;

    private LinearLayout layoutMessages;
    private EditText etMessage;
    private ListenerRegistration messagesListener;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUid = "";
        }

        requestId      = getIntent().getStringExtra("requestId");
        otherPersonName = getIntent().getStringExtra("otherPersonName");
        currentUserName = getIntent().getStringExtra("currentUserName");
        if (currentUserName == null) currentUserName = "Me";

        layoutMessages = findViewById(R.id.layoutMessages);
        etMessage      = findViewById(R.id.etMessage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        if (tvTitle != null && otherPersonName != null) tvTitle.setText(otherPersonName);

        View btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());

        if (requestId != null && !requestId.isEmpty()) {
            startListening();
        }
    }

    /**
     * Sends a new message to Firestore under the session request's messages sub-collection.
     */
    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        if (requestId == null || currentUid.isEmpty()) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderUid",   currentUid);
        msg.put("senderName",  currentUserName);
        msg.put("text",        text);
        msg.put("timestamp",   com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("sessionRequests")
                .document(requestId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    etMessage.setText("");
                })
                .addOnFailureListener(e ->
                        android.widget.Toast.makeText(this,
                                "Failed to send: " + e.getMessage(),
                                android.widget.Toast.LENGTH_SHORT).show());
    }

    /**
     * Attaches a real-time Firestore listener for the messages sub-collection,
     * ordered by timestamp ascending.
     */
    private void startListening() {
        messagesListener = db.collection("sessionRequests")
                .document(requestId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || layoutMessages == null) return;
                    layoutMessages.removeAllViews();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String senderUid  = doc.getString("senderUid");
                        String senderName = doc.getString("senderName");
                        String text       = doc.getString("text");
                        Date   ts         = doc.getDate("timestamp");

                        boolean isMine = currentUid.equals(senderUid);
                        addMessageBubble(senderName, text, ts, isMine);
                    }
                    // Scroll to bottom
                    View scrollView = findViewById(R.id.scrollMessages);
                    if (scrollView != null) scrollView.post(() ->
                            ((android.widget.ScrollView) scrollView).fullScroll(View.FOCUS_DOWN));
                });
    }

    /**
     * Dynamically adds a message bubble to the chat layout.
     * Sent messages appear on the right (purple), received on the left (white).
     */
    private void addMessageBubble(String senderName, String text, Date timestamp, boolean isMine) {
        if (layoutMessages == null || text == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);
        row.setGravity(isMine ? Gravity.END : Gravity.START);

        // Sender label (only for received messages)
        if (!isMine && senderName != null) {
            TextView tvName = new TextView(this);
            tvName.setText(senderName);
            tvName.setTextSize(11);
            tvName.setTextColor(0xFF8B97A8);
            tvName.setPadding(dp(4), 0, 0, dp(2));
            row.addView(tvName);
        }

        // Bubble card
        MaterialCardView bubble = new MaterialCardView(this);
        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.72);
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        bubble.setLayoutParams(bubbleParams);
        bubble.setMaxCardElevation(0);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}