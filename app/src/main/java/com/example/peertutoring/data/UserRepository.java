package com.example.peertutoring.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore operations for user profiles.
 * Implementation of User Stories 1, 2, 3, 4, 5, and 6.
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

    public interface LoadCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    /** US1: Saves a new student profile. */
    public void saveStudentProfile(@NonNull Student student, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(student.getUid())
                .set(student)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** US2: Saves a new tutor profile. */
    public void saveTutorProfile(@NonNull Tutor tutor, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(tutor.getUid())
                .set(tutor)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** US3: Updates profile fields. */
    public void updateProfile(@NonNull String uid, @NonNull Map<String, Object> updates, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** US5 & 6: Fetches potential tutors for ranking.
     * Note: We removed client-side sorting via Query to avoid "Missing Index" errors
     * and to allow for more complex ranking logic in memory.
     */
    public void getRecommendedTutors(@NonNull List<String> studentSubjects, @NonNull LoadCallback<List<DocumentSnapshot>> callback) {
        Query query = firestore.collection("users")
                .whereEqualTo("role", "tutor")
                .limit(20);

        if (!studentSubjects.isEmpty()) {
            query = query.whereArrayContainsAny("subjects", studentSubjects);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(queryDocumentSnapshots.getDocuments()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Fetches a user document by UID. */
    public void getUserProfile(@NonNull String uid, @NonNull LoadCallback<DocumentSnapshot> callback) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
