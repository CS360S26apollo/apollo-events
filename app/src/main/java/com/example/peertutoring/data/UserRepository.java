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

    public void updateProfile(@NonNull String uid, @NonNull Map<String, Object> updates, @NonNull SaveCallback callback) {
        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Client-side sorting avoids "Missing Index" errors and allows richer in-memory ranking.
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

    public void getUserProfile(@NonNull String uid, @NonNull LoadCallback<DocumentSnapshot> callback) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
