package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;

public class HomeDashboardActivity extends AppCompatActivity {

    Button btnProfile, btnSettings, btnLogout, btnNotifications;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        googleSignInClient = GoogleSignIn.getClient(this, new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build());

        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeDashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeDashboardActivity.this, "You have no new notifications.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignInClient.signOut().addOnCompleteListener(HomeDashboardActivity.this,
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(Task<Void> task) {
                                Toast.makeText(HomeDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(HomeDashboardActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        });
            }
        });
    }
}
