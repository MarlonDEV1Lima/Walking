package com.msystem.walking.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.msystem.walking.model.User;

public class UserRepository {
    private static UserRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<User> userLiveData;

    private UserRepository() {
        firestore = FirebaseFirestore.getInstance();
        userLiveData = new MutableLiveData<>();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public LiveData<User> getUserById(String userId) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        userLiveData.setValue(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // Log error
                    userLiveData.setValue(null);
                });

        return userLiveData;
    }

    public void getUserData(String userId, OnUserDataLoadedListener listener) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserDataLoaded(user);
                    } else {
                        listener.onUserDataLoaded(null);
                    }
                })
                .addOnFailureListener(e -> listener.onUserDataLoaded(null));
    }

    public void updateUser(User user) {
        if (user.getId() != null) {
            firestore.collection("users")
                    .document(user.getId())
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        userLiveData.setValue(user);
                    });
        }
    }

    public void createUser(User user) {
        firestore.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    user.setId(documentReference.getId());
                    userLiveData.setValue(user);
                });
    }

    public interface OnUserDataLoadedListener {
        void onUserDataLoaded(User user);
    }
}
