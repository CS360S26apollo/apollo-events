package com.example.peertutoring.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Firestore operations for user profiles.
 * This class serves as the Data Access Object (DAO) for the application,
 * centralizing all interactions with the Firebase Firestore database.
 * 
 * Role: Controller/Data Repository for User Profiles.
 * Implementation of User Stories 1, 2, 3, and 4.
 */
public class UserRepository {

    private final FirebaseFirestore firestore;

    /**
     * Initializes the repository with the default Firestore instance.
     */
    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Callback interface for asynchronous Firestore operations.
     */
    public interface SaveCallback {
        /** Called when the operation completes successfully. */
        void onSuccess();
        /** 
         * Called when the operation fails.
         * @param error Descriptive error message.
         */
        void onFailure(String error);
    }

    /**
     * US1: Saves a new student profile to Firestore.
     * @param student The student model to persist.
     * @param callback Result listener.
     */
    public void saveStudentProfile(@NonNull Student student, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(student.getUid())
                .set(student)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * US2: Saves a new tutor profile to Firestore.
     * @param tutor The tutor model to persist.
     * @param callback Result listener.
     */
    public void saveTutorProfile(@NonNull Tutor tutor, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(tutor.getUid())
                .set(tutor)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * US3: Updates specific fields in a user's profile.
     * @param uid The unique identifier of the user.
     * @param updates A map containing the fields and their new values.
     * @param callback Result listener.
     */
    public void updateProfile(@NonNull String uid,
                              @NonNull Map<String, Object> updates,
                              @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * US3: Updates privacy and visibility settings for a user.
     * @param uid The unique identifier of the user.
     * @param privacySettings Map of privacy-related fields (e.g., profileVisible).
     * @param callback Result listener.
     */
    public void updatePrivacy(@NonNull String uid,
                              @NonNull Map<String, Object> privacySettings,
                              @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(privacySettings)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * US4: Submits an identification document for tutor verification.
     * Updates the user's document URL and sets verification status to false (pending).
     * 
     * @param uid The unique identifier of the user.
     * @param idDocumentUrl The URL of the uploaded document.
     * @param callback Result listener.
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
     * Transitions a user from pending to verified status.
     * 
     * @param uid The unique identifier of the user.
     * @param callback Result listener.
     */
    public void verifyUser(@NonNull String uid, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update("verified", true)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}