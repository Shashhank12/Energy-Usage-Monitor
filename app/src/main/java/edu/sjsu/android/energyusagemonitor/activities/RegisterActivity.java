package edu.sjsu.android.energyusagemonitor.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        final EditText phoneEditText = findViewById(R.id.phone_register);
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
                String phone = "+" + phoneEditText.getText().toString().trim().replaceAll("[^0-9]", "");
                String password = passwordEditText.getText().toString();
                String password2 = passwordEditText2.getText().toString();

                boolean pwMatch = password.equals(password2);
                boolean reqMet = meetsPWRequirements(password);
                boolean phoneFormat = phone.matches("\\+\\d{11}");

                if (pwMatch && reqMet && phoneFormat) {
                    mAuth.createUserWithEmailAndPassword(username, password)
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Registration successful, move to HomeDashboardActivity
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            user.sendEmailVerification()
                                                            .addOnCompleteListener(verificationTask -> {
                                                                if (verificationTask.isSuccessful()) {
                                                                    saveUserToFirestore(user, firstname, lastname, username);
                                                                    promptEmailVerfication(user, phone);
                                                                }
                                                                else {
                                                                    showRegistrationFailed(R.string.registration_failed);
                                                                    deleteUserFromFirestore(user);
                                                                    mAuth.getCurrentUser().delete();
                                                                }
                                                            });
                                        }
                                    } else {
                                        showRegistrationFailed(R.string.registration_failed);
                                    }
                                }
                            });
                }
                else if (!reqMet) {
                    showRegistrationFailed(R.string.req_not_met);
                }
                else if (!phoneFormat) {
                    showRegistrationFailed(R.string.phone_format_wrong);
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

    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private String verificationId;
    private String verificationCode;
    private PhoneAuthCredential credential;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    RegisterActivity.this.credential = credential;
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                }

                @Override
                public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                    RegisterActivity.this.verificationId = verificationId;
                    RegisterActivity.this.forceResendingToken = token;

                    promptUserVerification();
                }
            };

    private void promptEmailVerfication(FirebaseUser user, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
        builder.setTitle("Verify Email");
        builder.setMessage("Please check your email to verify your account before proceeeding to 2FA. Press continue when ready.");
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button verify = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (verify != null) {
                    verify.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            user.reload().addOnCompleteListener(reload -> {
                                if (user.isEmailVerified()) {
                                    enroll2FA(user, phone);
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Please verify email first.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }

                Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (cancel != null) {
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showRegistrationFailed(R.string.registration_failed);
                            deleteUserFromFirestore(user);
                            mAuth.getCurrentUser().delete();
                            dialog.dismiss();
                        }
                    });
                }
            }
        });

        dialog.show();
    }

    private void promptUserVerification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
        builder.setTitle("Enter Phone Verification Code");
        final EditText codeInput = new EditText(RegisterActivity.this);
        codeInput.setHint("XXXXXX");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(codeInput);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button verify = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                verify.setText("Verify");
                verify.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!codeInput.getText().toString().trim().isEmpty()) {
                            verificationCode = codeInput.getText().toString().trim();
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                            MultiFactorAssertion multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential);
                            FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getMultiFactor()
                                    .enroll(multiFactorAssertion, "Phone Number")
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            mAuth.signOut();
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Wrong code. Try again.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "Fill in code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showRegistrationFailed(R.string.registration_failed);
                        deleteUserFromFirestore(FirebaseAuth.getInstance().getCurrentUser());
                        mAuth.getCurrentUser().delete();
                        dialog.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void enroll2FA(FirebaseUser user, String phoneNumber) {

        user.getMultiFactor().getSession()
                .addOnCompleteListener(new OnCompleteListener<MultiFactorSession>() {
                    @Override
                    public void onComplete(@NonNull Task<MultiFactorSession> task) {
                        if (task.isSuccessful()) {
                            MultiFactorSession multiFactorSession = task.getResult();

                            String phone = "+15555551234";

                            PhoneAuthOptions phoneAuthOptions = PhoneAuthOptions.newBuilder()
                                    .setPhoneNumber(phone)
                                    .setTimeout(30L, TimeUnit.SECONDS)
                                    .setMultiFactorSession(multiFactorSession)
                                    .setCallbacks(callbacks)
                                    .build();

                            PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions);
                        }
                    }
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

        if (pw.matches("^(?=.*[$*.{}()?!@#%&:;|]).*$")) {
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

    private void deleteUserFromFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.delete();
    }

    private void showRegistrationFailed(@StringRes int errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
