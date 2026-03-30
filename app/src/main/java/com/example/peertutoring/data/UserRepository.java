package com.example.peertutoring.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Firestore operations for user profiles.
 * Extended for US3: updateProfile and updatePrivacy.
 * Extended for US4: Verification badge methods.
 */
public class UserRepository {

    private final FirebaseFirestore firestore;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // ── US1: Save student on signup ───────────────────────────
    public void saveStudentProfile(@NonNull Student student, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(student.getUid())
                .set(student)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── US2: Save tutor on signup ─────────────────────────────
    public void saveTutorProfile(@NonNull Tutor tutor, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(tutor.getUid())
                .set(tutor)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── US3: Update specific profile fields ───────────────────
    public void updateProfile(@NonNull String uid,
                              @NonNull Map<String, Object> updates,
                              @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── US3: Update only visibility/privacy fields ────────────
    public void updatePrivacy(@NonNull String uid,
                              @NonNull Map<String, Object> privacySettings,
                              @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(privacySettings)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── US4: Identity Verification ────────────────────────────
    /**
     * Updates the user's document URL and sets verification status to false (pending).
     */
    public void submitVerificationId(@NonNull String uid,
                                   @NonNull String idDocumentUrl,
                                   @NonNull SaveCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("idDocumentUrl", idDocumentUrl);
        updates.put("verified", false); // Reset or set to false while pending

        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Admin method (mocked for now) to verify a user.
     */
    public void verifyUser(@NonNull String uid, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update("verified", true)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}