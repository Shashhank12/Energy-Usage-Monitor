package edu.sjsu.android.energyusagemonitor.ui.login;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.activities.HomeDashboardActivity;
import edu.sjsu.android.energyusagemonitor.activities.RegisterActivity;
import edu.sjsu.android.energyusagemonitor.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.signInButton.setOnClickListener(v -> forceGoogleAccountSelection());

        binding.login.setOnClickListener(v -> {
            String email = binding.username.getText().toString();
            String password = binding.password.getText().toString();
            binding.loading.setVisibility(ProgressBar.VISIBLE);


            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d("2FA", "signin success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.reload().addOnCompleteListener(reload -> {
                                    if (user.isEmailVerified()) {
                                        goToHome();
                                    }
                                    else {
                                        mAuth.signOut();
                                        showLoginFailed(R.string.not_verified);
                                    }
                                });
                            }
                        }
                        else {
                            Log.d("2FA", "signin fail");
                            Log.d("2FA", "entering 2FA");
                            check2FA(task);
                        }
                    });
        });

        binding.register.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
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
                    LoginActivity.this.credential = credential;
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {

                    } else if (e instanceof FirebaseTooManyRequestsException) {

                    }
                }

                @Override
                public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                    LoginActivity.this.verificationId = verificationId;
                    LoginActivity.this.forceResendingToken = token;
                    Log.d("2FA", "opening prompt");
                    promptUserVerification();
                }
            };

    private MultiFactorResolver multiFactorResolver;

    private void promptUserVerification() {
        Log.d("2FA", "inside prompt opening");
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Enter Phone Verification Code");
        final EditText codeInput = new EditText(LoginActivity.this);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(codeInput);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
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
                                            goToHome();
                                            dialogInterface.dismiss();
                                        }
                                    });
                                }
                            }
                            else {
                                mAuth.signOut();
                                showLoginFailed(R.string.not_verified);
                            }
                        });
            }
        });
        builder.setCancelable(false);

        builder.setNegativeButton("Cancel", null);

        Log.d("2FA", "building and showing prompt");
        builder.create().show();
    }

    private void check2FA(Task<AuthResult> task) {
        Log.d("2FA", "outside if statement");
        if (task.getException() instanceof FirebaseAuthMultiFactorException) {
            FirebaseAuthMultiFactorException e = (FirebaseAuthMultiFactorException) task.getException();
            multiFactorResolver = e.getResolver();
            Log.d("2FA", "after multiFactorResolver");
            MultiFactorInfo selectedHint = multiFactorResolver.getHints().get(0);
            Log.d("2FA", "after selectedHint");

            Log.d("2FA", "going to send sms and open prompt");
            PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder()
                    .setActivity(this)
                    .setMultiFactorSession(multiFactorResolver.getSession())
                    .setMultiFactorHint((PhoneMultiFactorInfo) selectedHint)
                    .setCallbacks(callbacks)
                    .setTimeout(30L, TimeUnit.SECONDS)
                    .build());
            Log.d("2FA", "after verifyPhoneNumber");
        }
    }

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
                    goToHome();
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


    private void goToHome() {
        startActivity(new Intent(this, HomeDashboardActivity.class));
        finish();
    }

    private void showLoginFailed(@StringRes int errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
