package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.databinding.ActivityLoginBinding;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final Button registerButton = findViewById(R.id.register_action);
        final Button loginHereButton = findViewById(R.id.login_here);
        final EditText firstnameEditText = findViewById(R.id.first_name_register);
        final EditText lastnameEditText = findViewById(R.id.last_name_register);
        final EditText usernameEditText = findViewById(R.id.username_register);
        final EditText passwordEditText = findViewById(R.id.password_register);
        final EditText passwordEditText2 = findViewById(R.id.password_register2);

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePWRequirements(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // empty
            }
        });

        passwordEditText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.toString()).equals(passwordEditText.getText().toString())) {
                    ((TextView) findViewById(R.id.match_req)).setTextColor(Color.GREEN);
                }
                else {
                    ((TextView) findViewById(R.id.match_req)).setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // empty
            }
        });

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstname = firstnameEditText.getText().toString();
                String lastname = lastnameEditText.getText().toString();
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String password2 = passwordEditText2.getText().toString();
                if (password.equals(password2) && meetsPWRequirements(password)) {
                    mAuth.createUserWithEmailAndPassword(username, password)
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Registration successful, move to HomeDashboardActivity
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            user.sendEmailVerification();
                                            saveUserToFirestore(user, firstname, lastname, username);
                                            mAuth.signOut();
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    } else {
                                        showRegistrationFailed(R.string.registration_failed);
                                    }
                                }
                            });
                }
                else {
                    showRegistrationFailed(R.string.password_no_match);
                }
            }
        });

        loginHereButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private boolean meetsPWRequirements(String pw) {
        String requirements = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^$*.{}()?!@#%;|]).{13,}$";
        return pw.matches(requirements);
    }

    private void updatePWRequirements(String pw) {
        if (pw.matches("^(?=.*[A-Z]).*$")) {
            ((TextView) findViewById(R.id.uppercase_req)).setTextColor(Color.GREEN);
        }
        else {
            ((TextView) findViewById(R.id.uppercase_req)).setTextColor(Color.RED);
        }

        if (pw.matches("^(?=.*[a-z]).*$")) {
            ((TextView) findViewById(R.id.lowercase_req)).setTextColor(Color.GREEN);
        }
        else {
            ((TextView) findViewById(R.id.lowercase_req)).setTextColor(Color.RED);
        }

        if (pw.matches("^(?=.*[0-9]).*$")) {
            ((TextView) findViewById(R.id.number_req)).setTextColor(Color.GREEN);
        }
        else {
            ((TextView) findViewById(R.id.number_req)).setTextColor(Color.RED);
        }

        if (pw.matches("^(?=.*[^$*.{}()?!@#%&:;|]).*$")) {
            ((TextView) findViewById(R.id.special_req)).setTextColor(Color.GREEN);
        }
        else {
            ((TextView) findViewById(R.id.special_req)).setTextColor(Color.RED);
        }

        if (pw.matches("^.{13,}$")) {
            ((TextView) findViewById(R.id.length_req)).setTextColor(Color.GREEN);
        }
        else {
            ((TextView) findViewById(R.id.length_req)).setTextColor(Color.RED);
        }
    }

    private void saveUserToFirestore(FirebaseUser user, String fn, String ln, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("users").document(user.getUid());

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("firstName", fn);
                profile.put("lastName", ln);
                profile.put("email", email);
                profile.put("budget", "250");

                docRef.set(profile, SetOptions.merge());
            }
        });
    }

    private void showRegistrationFailed(@StringRes int errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
