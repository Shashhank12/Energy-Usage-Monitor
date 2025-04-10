package edu.sjsu.android.energyusagemonitor.firestore;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";
    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ADD a document by ID
    public <T> void addDocument(String collectionPath, String documentId, T data, final FirestoreCallback<String> callback) {
        db.collection(collectionPath).document(documentId).set(data).addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document set with ID: " + documentId + " in collection: " + collectionPath);
                    if (callback != null) callback.onSuccess(documentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error setting document " + documentId + " in collection: " + collectionPath, e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // READ from Firestore
    public <T> void getDocumentById(String collectionPath, String documentId, Class<T> pojoClass, final FirestoreCallback<T> callback) {
        db.collection(collectionPath).document(documentId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            try {
                                T dataObject = document.toObject(pojoClass);
                                Log.d(TAG, "Document fetched: " + documentId + " from collection: " + collectionPath);
                                if (callback != null) callback.onSuccess(dataObject);
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document " + documentId + " to " + pojoClass.getSimpleName(), e);
                                if (callback != null) callback.onFailure(e);
                            }
                        } else {
                            Log.w(TAG, "No document found with ID: " + documentId + " in collection: " + collectionPath);
                            if (callback != null) callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting document: " + documentId + " from collection: " + collectionPath, task.getException());
                        if (callback != null) callback.onFailure(task.getException());
                    }
                });
    }

    public <T> void getAllDocuments(String collectionPath, Class<T> pojoClass, final FirestoreCallback<List<T>> callback) {
        db.collection(collectionPath)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<T> list = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                try {
                                    T dataObject = document.toObject(pojoClass);
                                    if (dataObject != null) {
                                        list.add(dataObject);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting a document in collection " + collectionPath + " to " + pojoClass.getSimpleName(), e);
                                }
                            }
                            Log.d(TAG, "Fetched " + list.size() + " documents from collection: " + collectionPath);
                            if (callback != null) callback.onSuccess(list);
                        } else {
                            Log.w(TAG, "QuerySnapshot was null for collection: " + collectionPath);
                            if (callback != null) callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        Log.e(TAG, "Error getting documents from collection: " + collectionPath, task.getException());
                        if (callback != null) callback.onFailure(task.getException());
                    }
                });
    }

    // UPDATE a document
    public void updateDocument(String collectionPath, String documentId, Map<String, Object> updates, final FirestoreSimpleCallback callback) {
        db.collection(collectionPath).document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document updated: " + documentId + " in collection: " + collectionPath);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating document " + documentId + " in collection: " + collectionPath, e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // DELETE a document
    public void deleteDocument(String collectionPath, String documentId, final FirestoreSimpleCallback callback) {
        db.collection(collectionPath).document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document deleted: " + documentId + " from collection: " + collectionPath);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting document " + documentId + " from collection: " + collectionPath, e);
                    if (callback != null) callback.onFailure(e);
                });
    }
}