package edu.sjsu.android.energyusagemonitor.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import edu.sjsu.android.energyusagemonitor.R;

public class EditProfileActivity extends AppCompatActivity {
    private EditText firstNameEdit, lastNameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firstNameEdit = findViewById(R.id.edit_first_name);
        lastNameEdit = findViewById(R.id.edit_last_name);
        Button saveButton = findViewById(R.id.btn_save);
        Button cancelButton = findViewById(R.id.btn_cancel);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedFirstName = prefs.getString("first_name", "");
        String savedLastName = prefs.getString("last_name", "");
        firstNameEdit.setText(savedFirstName);
        lastNameEdit.setText(savedLastName);

        saveButton.setOnClickListener(v -> {
            String newFirstName = firstNameEdit.getText().toString();
            String newLastName = lastNameEdit.getText().toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("first_name", newFirstName);
            editor.putString("last_name", newLastName);
            editor.apply();

            finish();
        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
