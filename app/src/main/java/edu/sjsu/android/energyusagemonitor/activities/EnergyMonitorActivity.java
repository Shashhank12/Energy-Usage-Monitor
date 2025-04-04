package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.uiBarchart.EnergyBarChartView;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

public class EnergyMonitorActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private EnergyBarChartView chartView;
    private Switch toggleSwitch;
    private TextView pageIndicatorText;
    private TextView summaryText;
    private int currentPage = 0;
    private List<List<BillsResponse.Bill>> paginatedBills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_monitor);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        chartView = findViewById(R.id.energy_bar_chart);
        toggleSwitch = findViewById(R.id.toggle_usage_cost);
        pageIndicatorText = findViewById(R.id.page_indicator);
        summaryText = findViewById(R.id.monthly_summary);

        Button prevBtn = findViewById(R.id.prev_button);
        Button nextBtn = findViewById(R.id.next_button);

        List<BillsResponse.Bill> allBills = new ArrayList<>(SettingsActivity.getBills());
        Collections.reverse(allBills);

        paginatedBills = paginateBills(allBills, 6);
        renderChart(currentPage, toggleSwitch.isChecked());

        toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                renderChart(currentPage, isChecked));

        nextBtn.setOnClickListener(v -> {
            if (currentPage < paginatedBills.size() - 1) {
                currentPage++;
                renderChart(currentPage, toggleSwitch.isChecked());
            }
        });

        prevBtn.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                renderChart(currentPage, toggleSwitch.isChecked());
            }
        });
    }

    private void renderChart(int pageIndex, boolean showCost) {
        List<BillsResponse.Bill> bills = paginatedBills.get(pageIndex);
        List<Float> data = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        float total = 0;

        for (BillsResponse.Bill bill : bills) {
            float value = (float) (showCost ? bill.getBase().getBillTotalCost() : bill.getBase().getBillTotalKwh());
            data.add(value);
            labels.add(bill.getBase().getBillStartDate().substring(0, 10));
            total += value;
        }

        chartView.setLabels(labels);
        chartView.setData(data);

        pageIndicatorText.setText("Page " + (pageIndex + 1) + " of " + paginatedBills.size());
        float average = total / bills.size();
        summaryText.setText(String.format("Average: %.2f %s", average, showCost ? "$" : "kWh"));
    }

    private List<List<BillsResponse.Bill>> paginateBills(List<BillsResponse.Bill> bills, int itemsPerPage) {
        List<List<BillsResponse.Bill>> pages = new ArrayList<>();
        for (int i = 0; i < bills.size(); i += itemsPerPage) {
            int end = Math.min(i + itemsPerPage, bills.size());
            pages.add(bills.subList(i, end));
        }
        return pages;
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
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(EnergyMonitorActivity.this, LoginActivity.class);
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
