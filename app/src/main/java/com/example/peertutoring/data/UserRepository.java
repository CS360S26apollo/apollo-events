package com.example.peertutoring.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.example.peertutoring.models.Student;
import com.example.peertutoring.models.Tutor;

/**
 * Handles Firestore operations for user profiles.
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

    public void saveStudentProfile(@NonNull Student student, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(student.getUid())
                .set(student)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void saveTutorProfile(@NonNull Tutor tutor, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(tutor.getUid())
                .set(tutor)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
