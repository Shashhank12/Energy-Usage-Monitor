package edu.sjsu.android.energyusagemonitor.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import edu.sjsu.android.energyusagemonitor.R;

public class EditProfileActivity extends AppCompatActivity {

    private EditText firstNameEdit, lastNameEdit, budgetEdit;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firstNameEdit = findViewById(R.id.edit_first_name);
        lastNameEdit = findViewById(R.id.edit_last_name);
        budgetEdit = findViewById(R.id.edit_budget);
        Button saveButton = findViewById(R.id.btn_save);
        Button cancelButton = findViewById(R.id.btn_cancel);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load current values
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                firstNameEdit.setText(doc.getString("firstName"));
                lastNameEdit.setText(doc.getString("lastName"));
                budgetEdit.setText(doc.getString("budget"));
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to fetch profile", e);
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
        });

        // Save updates
        saveButton.setOnClickListener(v -> {
            String newFirstName = firstNameEdit.getText().toString().trim();
            String newLastName = lastNameEdit.getText().toString().trim();
            String newBudget = budgetEdit.getText().toString().trim();

            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", newFirstName);
            updates.put("lastName", newLastName);
            updates.put("budget", newBudget);

            db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Profile updated");
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to ProfileActivity
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Update failed", e);
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    });
        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
