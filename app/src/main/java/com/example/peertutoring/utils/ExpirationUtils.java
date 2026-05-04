package com.example.peertutoring.utils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/**
 * Utility for US-11: auto-expiring session requests older than 3 days.
 *
 * Called from SessionRequestsActivity.onResume() (student side) and
 * TutorRequestsActivity.onResume() (tutor side) so expiration happens
 * whenever either party opens their requests screen.
 */
public class ExpirationUtils {

    public static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000;

    private static void expireStale(String filterField, String uid, FirebaseFirestore db) {
        if (uid == null || uid.isEmpty()) return;

        db.collection("sessionRequests")
                .whereEqualTo(filterField, uid)
                .whereEqualTo("status", "requested")
                .get()
                .addOnSuccessListener(snap -> {
                    long cutoff = System.currentTimeMillis() - THREE_DAYS_MS;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Date createdAt = doc.getDate("createdAt");
                        if (createdAt != null && createdAt.getTime() < cutoff) {
                            expireAndRefund(doc, db);
                        }
                    }
                });
    }

    public static void expireStaleRequestsForStudent(String studentUid, FirebaseFirestore db) {
        expireStale("studentUid", studentUid, db);
    }

    public static void expireStaleRequestsForTutor(String tutorUid, FirebaseFirestore db) {
        expireStale("tutorUid", tutorUid, db);
    }

    private static void expireAndRefund(DocumentSnapshot doc, FirebaseFirestore db) {
        String requestId  = doc.getId();
        String studentUid = doc.getString("studentUid");
        Long   tokens     = doc.getLong("tokens");

        db.collection("sessionRequests").document(requestId)
                .update("status", "expired")
                .addOnSuccessListener(u -> {
                    if (studentUid != null && tokens != null && tokens > 0) {
                        refundTokens(studentUid, tokens, db);
                    }
                });
    }

    private static void refundTokens(String studentUid, long amount, FirebaseFirestore db) {
        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(doc -> {
                    Long current = doc.getLong("tokens");
                    long newBalance = (current != null ? current : 0) + amount;
                    db.collection("users").document(studentUid)
                            .update("tokens", newBalance);
                });
    }
}
