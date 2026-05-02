package com.example.peertutoring.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shows all recent conversations the tutor has with students.
 * Queries conversations where participantA or participantB == tutorUid.
 * Tapping a conversation opens MessagingActivity on that thread.
 */
public class TutorChatListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tutorUid;
    private LinearLayout layoutChatList;
    private ListenerRegistration listenerA, listenerB;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_chat_list);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        tutorUid = user.getUid();

        layoutChatList = findViewById(R.id.layoutChatList);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadConversations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerA != null) listenerA.remove();
        if (listenerB != null) listenerB.remove();
    }

    // ── Load conversations ─────────────────────────────────────

    /**
     * Firestore doesn't support OR queries, so we run two queries:
     * 1. conversations where participantA == tutorUid
     * 2. conversations where participantB == tutorUid
     * Then merge and render.
     */
    private void loadConversations() {
        if (layoutChatList == null) return;

        final List<DocumentSnapshot> allConvs = new ArrayList<>();

        // Query 1: tutor is participantA
        listenerA = db.collection("conversations")
                .whereEqualTo("participantA", tutorUid)
                .addSnapshotListener((snapA, eA) -> {
                    allConvs.clear();
                    if (snapA != null) allConvs.addAll(snapA.getDocuments());

                    // Query 2: tutor is participantB — merge results
                    db.collection("conversations")
                            .whereEqualTo("participantB", tutorUid)
                            .get()
                            .addOnSuccessListener(snapB -> {
                                if (snapB != null) {
                                    for (DocumentSnapshot doc : snapB.getDocuments()) {
                                        // Avoid duplicates
                                        boolean already = false;
                                        for (DocumentSnapshot d : allConvs) {
                                            if (d.getId().equals(doc.getId())) {
                                                already = true; break;
                                            }
                                        }
                                        if (!already) allConvs.add(doc);
                                    }
                                }
                                // Sort by lastMessageAt descending (most recent first)
                                allConvs.sort((a, b) -> {
                                    com.google.firebase.Timestamp ta =
                                            a.getTimestamp("lastMessageAt");
                                    com.google.firebase.Timestamp tb =
                                            b.getTimestamp("lastMessageAt");
                                    if (ta == null && tb == null) return 0;
                                    if (ta == null) return 1;
                                    if (tb == null) return -1;
                                    return tb.compareTo(ta);
                                });
                                renderConversations(allConvs);
                            });
                });
    }

    // ── Render ────────────────────────────────────────────────

    private void renderConversations(List<DocumentSnapshot> convs) {
        if (layoutChatList == null) return;
        layoutChatList.removeAllViews();

        if (convs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No conversations yet.\nStart chatting from a session request.");
            empty.setTextColor(0xFF8B97A8);
            empty.setTextSize(15f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(80), 0, 0);
            empty.setLineSpacing(dp(6), 1f);
            layoutChatList.addView(empty);
            return;
        }

        for (DocumentSnapshot conv : convs) {
            String convId      = conv.getId();
            String lastMsg     = conv.getString("lastMessage");
            String otherName   = getOtherPersonName(conv);
            Date   lastMsgAt   = conv.getDate("lastMessageAt");

            // Build card
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));

            // Avatar circle with initials
            MaterialCardView avatar = new MaterialCardView(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(48), dp(48));
            ap.setMarginEnd(dp(12));
            avatar.setLayoutParams(ap);
            avatar.setRadius(dp(24));
            avatar.setCardElevation(0);
            avatar.setCardBackgroundColor(0xFF8A2EFF);

            TextView tvInitials = new TextView(this);
            String initials = otherName != null && !otherName.isEmpty()
                    ? String.valueOf(otherName.charAt(0)).toUpperCase() : "?";
            tvInitials.setText(initials);
            tvInitials.setTextColor(0xFFFFFFFF);
            tvInitials.setTextSize(18f);
            tvInitials.setTypeface(null, Typeface.BOLD);
            tvInitials.setGravity(Gravity.CENTER);
            tvInitials.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            avatar.addView(tvInitials);
            row.addView(avatar);

            // Name + last message
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(otherName != null ? otherName : "Student");
            tvName.setTextColor(0xFF071A3D);
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);
            info.addView(tvName);

            TextView tvPreview = new TextView(this);
            tvPreview.setText(lastMsg != null ? lastMsg : "Tap to open chat");
            tvPreview.setTextColor(0xFF8B97A8);
            tvPreview.setTextSize(13f);
            tvPreview.setMaxLines(1);
            tvPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvPreview);

            row.addView(info);

            // Timestamp
            if (lastMsgAt != null) {
                TextView tvTime = new TextView(this);
                tvTime.setText(TIME_FMT.format(lastMsgAt));
                tvTime.setTextColor(0xFF8B97A8);
                tvTime.setTextSize(11f);
                tvTime.setGravity(Gravity.END);
                row.addView(tvTime);
            }

            card.addView(row);

            // Open MessagingActivity on tap
            final String fConvId    = convId;
            final String fOtherName = otherName;
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, MessagingActivity.class);
                intent.putExtra("requestId",       fConvId);   // stable convId
                intent.putExtra("otherPersonName", fOtherName != null ? fOtherName : "Student");
                startActivity(intent);
            });

            layoutChatList.addView(card);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * The convId is "uidA_uidB" (sorted). The other person's name is stored
     * as "otherPersonName" when the conversation was created from TutorDetailActivity.
     * If not set, we look up the name from the users collection.
     */
    /**
     * Returns the OTHER person's name from the tutor's perspective.
     * Conversations store both tutorName and studentName explicitly.
     * Tutor always wants to see the STUDENT name, not their own.
     */
    private String getOtherPersonName(DocumentSnapshot conv) {
        // Best: use studentName stored when conversation was created
        String studentName = conv.getString("studentName");
        if (studentName != null && !studentName.isEmpty()) return studentName;

        // Fallback: derive the other UID and look up from Firestore
        String convId = conv.getId();
        String[] parts = convId.split("_", 2);
        if (parts.length < 2) return "Student";

        // The other UID is whichever part is NOT the tutor's UID
        String otherUid = parts[0].equals(tutorUid) ? parts[1] : parts[0];

        // Async lookup — update Firestore and re-render once resolved
        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("fullName");
                        if (name == null || name.isEmpty())
                            name = userDoc.getString("firstName");
                        if (name != null && !name.isEmpty()) {
                            // Cache in conversation doc for next time
                            conv.getReference().update("studentName", name);
                            loadConversations(); // refresh list with correct name
                        }
                    }
                });

        return "Student";
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}