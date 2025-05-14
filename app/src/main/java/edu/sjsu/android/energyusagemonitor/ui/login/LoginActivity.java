package edu.sjsu.android.energyusagemonitor.ui.login;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.activities.RegisterActivity;
import edu.sjsu.android.energyusagemonitor.activities.SettingsActivity;
import edu.sjsu.android.energyusagemonitor.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private String verificationId;
    private String verificationCode;
    private PhoneAuthCredential credential;
    private MultiFactorResolver multiFactorResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPrivacyPolicyTextView();
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.signInButton.setOnClickListener(v -> forceGoogleAccountSelection());

        findViewById(R.id.forgot_password).setOnClickListener(v -> showResetPasswordDialog());

        binding.login.setOnClickListener(v -> {
            String email = binding.username.getText().toString();
            String password = binding.password.getText().toString();
            binding.loading.setVisibility(ProgressBar.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        binding.loading.setVisibility(ProgressBar.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.reload().addOnCompleteListener(reload -> {
                                    if (user.isEmailVerified()) {
                                        goToSettings();
                                    } else {
                                        mAuth.signOut();
                                        showLoginFailed(R.string.not_verified);
                                    }
                                });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e != null) {
                                e.printStackTrace();

                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(this, "No account found with this email.", Toast.LENGTH_LONG).show();
                                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(this, "Incorrect password.", Toast.LENGTH_LONG).show();
                                } else if (e instanceof FirebaseAuthMultiFactorException) {
                                    check2FA(task);
                                } else {
                                    Toast.makeText(this, "Login failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(this, "Login failed for unknown reasons.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }); // ✅ Fixed missing semicolon here
        });

        binding.register.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(emailInput);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Password reset email sent.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to send reset email.", Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void setupPrivacyPolicyTextView() {
        TextView privacyPolicyTextView = findViewById(R.id.privacy_policy_text_login);

        String privacyText = "By signing up, you agree to the Privacy Policy.";
        SpannableString spannableString = new SpannableString(privacyText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Privacy Policy");

                WebView webView = new WebView(LoginActivity.this);
                webView.loadUrl("https://shashhank12.github.io/EnergyUsageThirdPartyPortal/");
                webView.setWebViewClient(new WebViewClient());

                builder.setView(webView);
                builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        };

        int startIndex = privacyText.indexOf("Privacy Policy");
        int endIndex = startIndex + "Privacy Policy".length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        privacyPolicyTextView.setText(spannableString);
        privacyPolicyTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void promptUserVerification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Enter Phone Verification Code");
        final EditText codeInput = new EditText(LoginActivity.this);
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

                            multiFactorResolver.resolveSignIn(multiFactorAssertion)
                                    .addOnCompleteListener(resolveTask -> {
                                        if (resolveTask.isSuccessful()) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                user.reload().addOnCompleteListener(reload -> {
                                                    if (user.isEmailVerified()) {
                                                        goToSettings();
                                                        dialog.dismiss();
                                                    }
                                                });
                                            }
                                        } else {
                                            showLoginFailed(R.string.not_verified);
                                        }
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "Fill in code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancel.setText("Cancel");
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mAuth.signOut();
                        dialog.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void check2FA(Task<AuthResult> task) {
        if (task.getException() instanceof FirebaseAuthMultiFactorException) {
            FirebaseAuthMultiFactorException e = (FirebaseAuthMultiFactorException) task.getException();
            multiFactorResolver = e.getResolver();
            MultiFactorInfo selectedHint = multiFactorResolver.getHints().get(0);

            PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder()
                    .setActivity(this)
                    .setMultiFactorSession(multiFactorResolver.getSession())
                    .setMultiFactorHint((PhoneMultiFactorInfo) selectedHint)
                    .setCallbacks(callbacks)
                    .setTimeout(30L, TimeUnit.SECONDS)
                    .build());
        }
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    LoginActivity.this.credential = credential;
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    e.printStackTrace();
                }

                @Override
                public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                    LoginActivity.this.verificationId = verificationId;
                    LoginActivity.this.forceResendingToken = token;
                    promptUserVerification();
                }
            };

    private void forceGoogleAccountSelection() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            showLoginFailed(R.string.login_failed);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveUserToFirestore(user, account);
                    goToSettings();
                }
            } else {
                showLoginFailed(R.string.login_failed);
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user, GoogleSignInAccount account) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] names = account.getDisplayName() != null
                ? account.getDisplayName().split(" ", 2)
                : new String[]{"", ""};

        DocumentReference docRef = db.collection("users").document(user.getUid());

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("firstName", names.length > 0 ? names[0] : "");
                profile.put("lastName", names.length > 1 ? names[1] : "");
                profile.put("email", account.getEmail());
                profile.put("budget", "250");

                docRef.set(profile, SetOptions.merge());
            }
        });
    }

    private void goToSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
    }

    private void showLoginFailed(@StringRes int errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
