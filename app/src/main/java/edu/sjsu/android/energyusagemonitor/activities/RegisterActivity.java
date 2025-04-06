package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.sjsu.android.energyusagemonitor.R;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final Button registerButton = findViewById(R.id.register_action);
        final EditText usernameEditText = findViewById(R.id.username_register);
        final EditText passwordEditText = findViewById(R.id.password_register);

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                mAuth.createUserWithEmailAndPassword(username, password)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Registration successful, move to HomeDashboardActivity
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        Intent intent = new Intent(RegisterActivity.this, HomeDashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            }
                        });
            }
        });
    }
}
