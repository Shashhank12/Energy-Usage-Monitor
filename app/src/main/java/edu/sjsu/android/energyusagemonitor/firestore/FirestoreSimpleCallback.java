package edu.sjsu.android.energyusagemonitor.firestore;

public interface FirestoreSimpleCallback {
    void onSuccess();
    void onFailure(Exception e);
}
