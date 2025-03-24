package edu.sjsu.android.energyusagemonitor.activities;

import android.app.Notification;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.energyusagemonitor.R;

public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        List<Notification> notifications = getNotificationsFromDatabase();
        if (notifications.isEmpty()) {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
        } else {
            // Display the notifications
        }
    }

    private List<Notification> getNotificationsFromDatabase() {
        return new ArrayList<>();
    }
}
