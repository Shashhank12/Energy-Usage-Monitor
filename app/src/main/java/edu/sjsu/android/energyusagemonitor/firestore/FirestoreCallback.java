package edu.sjsu.android.energyusagemonitor.firestore;

public interface FirestoreCallback<T> {
    void onSuccess(T result);
    void onFailure(Exception e);
}

