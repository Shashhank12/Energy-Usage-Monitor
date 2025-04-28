package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;

public class ProfileActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView firstNameText, lastNameText, emailText, budgetText;
    private ImageView profileImageView;

    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        firstNameText = findViewById(R.id.first_name_text);
        lastNameText = findViewById(R.id.last_name_text);
        emailText = findViewById(R.id.email_text);
        budgetText = findViewById(R.id.budget_text);
        profileImageView = findViewById(R.id.profile_image);

        user = FirebaseAuth.getInstance().getCurrentUser();

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Glide.with(this).load(imageUri).into(profileImageView);

                        if (user != null) {
                            Map<String, Object> update = new HashMap<>();
                            update.put("profilePic", imageUri.toString());
                            db.collection("users").document(user.getUid())
                                    .update(update)
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
                        }
                    }
                });

        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerLauncher.launch(intent);
        });

        loadUserProfile();

        Button editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        Button deleteButton = findViewById(R.id.delete_account_button);
        deleteButton.setOnClickListener(v -> {
            if (user != null) {
                // Delete Firestore user document first
                db.collection("users").document(user.getUid()).delete()
                        .addOnSuccessListener(aVoid -> {
                            // Now delete FirebaseAuth account
                            user.delete()
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(ProfileActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to remove user data", Toast.LENGTH_SHORT).show());
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            firstNameText.setText(doc.getString("firstName"));
                            lastNameText.setText(doc.getString("lastName"));
                            emailText.setText(doc.getString("email"));
                            budgetText.setText(doc.getString("budget"));

                            String imageUrl = doc.getString("profilePic");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this).load(Uri.parse(imageUrl)).into(profileImageView);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_home_dashboard) {
            startActivity(new Intent(this, HomeDashboardActivity.class));
        } else if (id == R.id.nav_energy_monitor) {
            startActivity(new Intent(this, EnergyMonitorActivity.class));
        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            GoogleSignInClient googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
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
