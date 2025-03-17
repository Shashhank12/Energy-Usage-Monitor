package edu.sjsu.android.energyusagemonitor.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.sjsu.android.energyusagemonitor.data.model.LoggedInUser;

public class LoginDataSource {

    private FirebaseAuth mAuth;

    public LoginDataSource() {
        mAuth = FirebaseAuth.getInstance();
    }

    public void login(String username, String password, final LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                LoggedInUser loggedInUser = new LoggedInUser(user.getUid(), user.getEmail());
                                callback.onSuccess(loggedInUser);
                            } else {
                                callback.onError(new Exception("User is null"));
                            }
                        } else {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    public void logout() {
        mAuth.signOut();
    }

    public interface LoginCallback {
        void onSuccess(LoggedInUser user);
        void onError(Exception exception);
    }
}