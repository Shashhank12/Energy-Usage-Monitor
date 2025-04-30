package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        final EditText usernameEditText = findViewById(R.id.username_register);
        final EditText passwordEditText = findViewById(R.id.password_register);
        final EditText passwordEditText2 = findViewById(R.id.password_register2);


        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // empty
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // empty
            }
        });

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                            Intent intent = new Intent(RegisterActivity.this, HomeDashboardActivity.class);
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
        String requirements = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^$*.{}()?!@#%&,><':;|`]).{13,}$";
        return pw.matches(requirements);
    }

    private void showRegistrationFailed(@StringRes int errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
