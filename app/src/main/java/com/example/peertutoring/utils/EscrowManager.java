package com.example.peertutoring.utils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * US-24: Manages token escrow for session payments.
 *
 * Flow:
 *  1. deductFromStudent()  — deducts tokens from student, marks as HELD
 *  2. releaseToTutor()     — on completion, reads tokens from session doc
 *                            and transfers them to the tutor
 *  3. refundToStudent()    — on cancellation/decline, returns tokens to student
 *  4. atomicRefund()       — lightweight refund without callback (instant book failure)
 */
public class EscrowManager {

    public static final String PAYMENT_HELD     = "HELD";
    public static final String PAYMENT_RELEASED = "RELEASED";
    public static final String PAYMENT_REFUNDED = "REFUNDED";

    public interface OnSuccess { void run(); }
    public interface OnFailure { void run(); }

    /**
     * Atomically deducts tokens from the student's balance and moves them into escrow.
     * Uses a Firestore transaction so concurrent booking attempts cannot both pass the
     * balance check and cause a negative balance (the silent double-spend bug).
     * The transaction aborts — triggering onFailure — if the balance is insufficient.
     */
    public static void deductFromStudent(FirebaseFirestore db,
                                         String studentUid,
                                         int tokenCost,
                                         OnSuccess onSuccess,
                                         OnFailure onFailure) {
        DocumentReference userRef = db.collection("users").document(studentUid);

        db.runTransaction(transaction -> {
            // All reads before all writes (Firestore transaction requirement)
            com.google.firebase.firestore.DocumentSnapshot doc = transaction.get(userRef);

            Long current = doc.getLong("tokens");
            long balance = (current != null) ? current : 0L;

            if (balance < tokenCost) {
                // Abort the transaction — triggers addOnFailureListener
                throw new FirebaseFirestoreException(
                        "Insufficient token balance: " + balance + " < " + tokenCost,
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Long existingEscrow = doc.getLong("escrowBalance");
            Map<String, Object> updates = new HashMap<>();
            updates.put("tokens",        balance - tokenCost);
            updates.put("escrowBalance", (existingEscrow != null ? existingEscrow : 0L) + tokenCost);
            transaction.update(userRef, updates);
            return null;
        })
        .addOnSuccessListener(unused -> onSuccess.run())
        .addOnFailureListener(e -> onFailure.run());
    }

    public static void releaseToTutor(FirebaseFirestore db,
                                      String requestId,
                                      String tutorUid,
                                      OnSuccess onSuccess) {
        if (requestId == null || requestId.isEmpty()) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        db.collection("sessionRequests").document(requestId).get()
                .addOnSuccessListener(sessionDoc -> {
                    Long tokLong    = sessionDoc.getLong("tokens");
                    int  tokenCost  = (tokLong != null) ? tokLong.intValue() : 0;
                    String studentUid = sessionDoc.getString("studentUid");

                    if (tutorUid != null && !tutorUid.isEmpty() && tokenCost > 0) {
                        db.collection("users").document(tutorUid).get()
                                .addOnSuccessListener(doc -> {
                                    Long cur = doc.getLong("tokens");
                                    long newBal = (cur != null ? cur : 0L) + tokenCost;
                                    db.collection("users").document(tutorUid)
                                            .update("tokens", newBal);
                                });
                    }

                    if (studentUid != null && !studentUid.isEmpty() && tokenCost > 0) {
                        db.collection("users").document(studentUid).get()
                                .addOnSuccessListener(doc -> {
                                    Long escrow = doc.getLong("escrowBalance");
                                    long newEscrow = Math.max(0,
                                            (escrow != null ? escrow : 0L) - tokenCost);
                                    db.collection("users").document(studentUid)
                                            .update("escrowBalance", newEscrow);
                                });
                    }

                    db.collection("sessionRequests").document(requestId)
                            .update("paymentStatus", PAYMENT_RELEASED)
                            .addOnSuccessListener(u -> {
                                if (onSuccess != null) onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                if (onSuccess != null) onSuccess.run(); // non-critical
                            });
                })
                .addOnFailureListener(e -> {
                    if (onSuccess != null) onSuccess.run();
                });
    }

    public static void releaseToTutor(FirebaseFirestore db,
                                      String studentUid,
                                      String tutorUid,
                                      int tokenCost,
                                      String requestId) {
        if (tutorUid != null && !tutorUid.isEmpty()) {
            db.collection("users").document(tutorUid).get()
                    .addOnSuccessListener(doc -> {
                        Long cur = doc.getLong("tokens");
                        db.collection("users").document(tutorUid)
                                .update("tokens", (cur != null ? cur : 0L) + tokenCost);
                    });
        }

        if (studentUid != null && !studentUid.isEmpty()) {
            db.collection("users").document(studentUid).get()
                    .addOnSuccessListener(doc -> {
                        Long escrow = doc.getLong("escrowBalance");
                        long newEscrow = Math.max(0, (escrow != null ? escrow : 0L) - tokenCost);
                        db.collection("users").document(studentUid)
                                .update("escrowBalance", newEscrow);
                    });
        }

        if (requestId != null && !requestId.isEmpty()) {
            db.collection("sessionRequests").document(requestId)
                    .update("paymentStatus", PAYMENT_RELEASED);
        }
    }

    public static void refundToStudent(FirebaseFirestore db,
                                       String requestId,
                                       String studentUid,
                                       int tokensToRefund,
                                       OnSuccess onSuccess) {
        if (studentUid == null || studentUid.isEmpty()) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(doc -> {
                    Long current = doc.getLong("tokens");
                    Long escrow  = doc.getLong("escrowBalance");

                    long newBalance = (current != null ? current : 0L) + tokensToRefund;
                    long newEscrow  = Math.max(0, (escrow != null ? escrow : 0L) - tokensToRefund);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tokens",        newBalance);
                    updates.put("escrowBalance", newEscrow);

                    db.collection("users").document(studentUid)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(u -> {
                                if (requestId != null && !requestId.isEmpty()) {
                                    db.collection("sessionRequests").document(requestId)
                                            .update("paymentStatus", PAYMENT_REFUNDED);
                                }
                                if (onSuccess != null) onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                if (onSuccess != null) onSuccess.run();
                            });
                })
                .addOnFailureListener(e -> {
                    if (onSuccess != null) onSuccess.run();
                });
    }

    /**
     * Fire-and-forget refund. Used when an instant book fails after
     * tokens were already deducted.
     */
    public static void atomicRefund(FirebaseFirestore db,
                                    String studentUid,
                                    int tokenCost) {
        if (studentUid == null || studentUid.isEmpty()) return;

        db.collection("users").document(studentUid).get()
                .addOnSuccessListener(doc -> {
                    Long current = doc.getLong("tokens");
                    Long escrow  = doc.getLong("escrowBalance");

                    long newBalance = (current != null ? current : 0L) + tokenCost;
                    long newEscrow  = Math.max(0, (escrow != null ? escrow : 0L) - tokenCost);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tokens",        newBalance);
                    updates.put("escrowBalance", newEscrow);

                    db.collection("users").document(studentUid)
                            .set(updates, SetOptions.merge());
                });
    }

    /** Overload with requestId — also marks payment as REFUNDED. */
    public static void atomicRefund(FirebaseFirestore db,
                                    String studentUid,
                                    int tokenCost,
                                    String requestId) {
        atomicRefund(db, studentUid, tokenCost);
        if (requestId != null && !requestId.isEmpty()) {
            db.collection("sessionRequests").document(requestId)
                    .update("paymentStatus", PAYMENT_REFUNDED);
        }
    }
}