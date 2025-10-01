package com.msystem.walking.repository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.msystem.walking.model.WalkSession;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WalkRepository {

    private static WalkRepository instance;
    private final FirebaseFirestore db;

    private WalkRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized WalkRepository getInstance() {
        if (instance == null) {
            instance = new WalkRepository();
        }
        return instance;
    }

    public void startWalkSession(WalkSession walkSession) {
        db.collection("walk_sessions")
                .add(walkSession)
                .addOnSuccessListener(documentReference -> {
                    walkSession.setId(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    public void stopWalkSession(WalkSession walkSession) {
        if (walkSession.getId() != null) {
            walkSession.setEndTime(new Date());
            db.collection("walk_sessions")
                    .document(walkSession.getId())
                    .set(walkSession)
                    .addOnFailureListener(e -> {
                        // Handle error
                    });
        }
    }

    public void getTodayDistance(String userId, OnDistanceLoadedListener listener) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Date startOfDay = today.getTime();

        today.add(Calendar.DAY_OF_MONTH, 1);
        Date startOfNextDay = today.getTime();

        db.collection("walk_sessions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalDistance = 0.0;
                    List<WalkSession> sessions = queryDocumentSnapshots.toObjects(WalkSession.class);
                    for (WalkSession session : sessions) {
                        totalDistance += session.getDistance();
                    }
                    listener.onDistanceLoaded(totalDistance);
                })
                .addOnFailureListener(e -> listener.onDistanceLoaded(0.0));
    }

    public void getUserWalkHistory(String userId, OnWalkHistoryLoadedListener listener) {
        db.collection("walk_sessions")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WalkSession> sessions = queryDocumentSnapshots.toObjects(WalkSession.class);
                    listener.onWalkHistoryLoaded(sessions);
                })
                .addOnFailureListener(e -> listener.onWalkHistoryLoaded(null));
    }

    public interface OnDistanceLoadedListener {
        void onDistanceLoaded(double distance);
    }

    public interface OnWalkHistoryLoadedListener {
        void onWalkHistoryLoaded(List<WalkSession> sessions);
    }
}

