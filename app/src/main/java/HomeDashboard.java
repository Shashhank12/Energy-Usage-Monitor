import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HomeDashboardActivity extends AppCompatActivity {

    Button btnProfile, btnSettings, btnLogout, btnNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        // Initialize the buttons
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);

        // Set up click listeners for the buttons

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
    }
}

