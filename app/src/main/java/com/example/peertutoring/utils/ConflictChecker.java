package com.example.peertutoring.utils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/**
 * US-14: Detects scheduling conflicts before a session is booked or requested.
 *
 * Conflict rules:
 *  - Tutor: no session may overlap an existing active (requested/booked) session,
 *    accounting for the tutor's configured buffer time from tutorAvailability.
 *  - Student: no two sessions may overlap (exact window, no buffer).
 *
 * Queries all sessions for the given UID and filters status in-memory to avoid
 * requiring composite Firestore indexes.
 */
public class ConflictChecker {

    public interface ConflictCallback {
        void onResult(boolean hasConflict, String reason);
    }

    /**
     * Checks for scheduling conflicts for both the tutor (with buffer) and the student
     * (exact overlap). Calls the callback on the calling thread (Firestore main-thread callback).
     *
     * @param db                Firestore instance
     * @param tutorUid          Tutor's UID
     * @param studentUid        Student's UID
     * @param proposedStart     Proposed session start time
     * @param durationMinutes   Proposed session duration in minutes
     * @param excludeRequestId  Request ID to skip in the check (for reschedule; null for new sessions)
     * @param callback          Delivers (hasConflict, reason) — reason is null when no conflict
     */
    public static void checkConflict(
            FirebaseFirestore db,
            String tutorUid,
            String studentUid,
            Date proposedStart,
            int durationMinutes,
            String excludeRequestId,
            ConflictCallback callback) {

        long proposedStartMs = proposedStart.getTime();
        long proposedEndMs   = proposedStartMs + (long) durationMinutes * 60_000L;

        // Fetch tutor's buffer, then chain the two checks.
        db.collection("tutorAvailability").document(tutorUid).get()
                .addOnSuccessListener(availDoc -> {
                    Long bufferLong = availDoc.exists() ? availDoc.getLong("bufferMinutes") : null;
                    long bufferMs   = (bufferLong != null ? bufferLong : 0L) * 60_000L;
                    runTutorCheck(db, tutorUid, proposedStartMs, proposedEndMs, bufferMs,
                            excludeRequestId, studentUid, callback);
                })
                .addOnFailureListener(e ->
                        runTutorCheck(db, tutorUid, proposedStartMs, proposedEndMs, 0L,
                                excludeRequestId, studentUid, callback));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void runTutorCheck(
            FirebaseFirestore db,
            String tutorUid,
            long proposedStartMs,
            long proposedEndMs,
            long bufferMs,
            String excludeId,
            String studentUid,
            ConflictCallback callback) {

        db.collection("sessionRequests")
                .whereEqualTo("tutorUid", tutorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (excludeId != null && doc.getId().equals(excludeId)) continue;
                        if (!isActive(doc)) continue;

                        Date existingStart = doc.getDate("scheduledDate");
                        if (existingStart == null) continue;
                        Long dur = doc.getLong("durationMinutes");
                        if (dur == null) continue;

                        long existingStartMs = existingStart.getTime();
                        long existingEndMs   = existingStartMs + dur * 60_000L;

                        // Block zone: [existingStart - buffer, existingEnd + buffer]
                        if (proposedStartMs < existingEndMs + bufferMs
                                && proposedEndMs > existingStartMs - bufferMs) {
                            String bufferNote = bufferMs > 0
                                    ? " (includes " + (bufferMs / 60_000L) + "-min buffer)" : "";
                            callback.onResult(true,
                                    "Tutor already has a session at that time" + bufferNote + ".");
                            return;
                        }
                    }
                    // Tutor OK — now check student.
                    runStudentCheck(db, studentUid, proposedStartMs, proposedEndMs, excludeId, callback);
                })
                .addOnFailureListener(e ->
                        runStudentCheck(db, studentUid, proposedStartMs, proposedEndMs, excludeId, callback));
    }

    private static void runStudentCheck(
            FirebaseFirestore db,
            String studentUid,
            long proposedStartMs,
            long proposedEndMs,
            String excludeId,
            ConflictCallback callback) {

        db.collection("sessionRequests")
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (excludeId != null && doc.getId().equals(excludeId)) continue;
                        if (!isActive(doc)) continue;

                        Date existingStart = doc.getDate("scheduledDate");
                        if (existingStart == null) continue;
                        Long dur = doc.getLong("durationMinutes");
                        if (dur == null) continue;

                        long existingStartMs = existingStart.getTime();
                        long existingEndMs   = existingStartMs + dur * 60_000L;

                        if (proposedStartMs < existingEndMs && proposedEndMs > existingStartMs) {
                            callback.onResult(true, "You already have a session at that time.");
                            return;
                        }
                    }
                    callback.onResult(false, null);
                })
                .addOnFailureListener(e -> callback.onResult(false, null));
    }

    /** Returns true if the session status is 'requested' or 'booked'. */
    private static boolean isActive(DocumentSnapshot doc) {
        String status = doc.getString("status");
        return "requested".equals(status) || "booked".equals(status);
    }
}
