package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreCallback;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreRepository;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreSimpleCallback;
import edu.sjsu.android.energyusagemonitor.firestore.TestUser;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;

public class HomeDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        firestoreRepository = new FirestoreRepository();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void createTestUser() {
        TestUser user = new TestUser(FirebaseAuth.getInstance().getCurrentUser().getEmail(), "TestFirstName", "TestLastName");

        firestoreRepository.addDocument("TestUser", FirebaseAuth.getInstance().getCurrentUser().getUid(), user, new FirestoreCallback<String>() {
            @Override
            public void onSuccess(String documentId) {}

            @Override
            public void onFailure(Exception e) {}
        });
    }

    private TestUser currentUser;

    private void getTestUser() {
        firestoreRepository.getDocumentById("TestUser", FirebaseAuth.getInstance().getCurrentUser().getUid(), TestUser.class, new FirestoreCallback<TestUser>() {
            @Override
            public void onSuccess(TestUser testUser) {
                Log.wtf("TestUser", "TestUser: " + testUser.getFirstName());
                currentUser = testUser;
            }

            @Override
            public void onFailure(Exception e) {}
        });
    }

    private void updateTestUser() {
        Map<String, Object> updates = Map.of(
                "firstName", "UpdatedFirstName",
                "lastName", "UpdatedLastName"
        );

        firestoreRepository.updateDocument("TestUser", FirebaseAuth.getInstance().getCurrentUser().getUid(), updates, new FirestoreSimpleCallback() {
            @Override
            public void onSuccess() {
                Log.wtf("TestUser", "TestUser updated successfully");
            }

            @Override
            public void onFailure(Exception e) {
                Log.wtf("TestUser", "Failed to update TestUser: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_home_dashboard) {
            startActivity(new Intent(this, HomeDashboardActivity.class));
        }else if (id == R.id.nav_energy_monitor) {
            startActivity(new Intent(this, EnergyMonitorActivity.class));
        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(HomeDashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
