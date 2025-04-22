package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.uiBarchart.EnergyBarChartView;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

public class EnergyMonitorActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private EnergyBarChartView chartView;
    private Switch toggleSwitch;
    private TextView pageIndicatorText;
    private TextView summaryText;
    private Button prevBtn;
    private Button nextBtn;
    private int currentPage = 0;
    private int itemsPerPage = 6;

    private PgeDataManager pgeDataManager;
    private PgeDataManager.DataSource currentDataSource;

    private List<List<BillsResponse.Bill>> paginatedApiBills = new ArrayList<>();
    private List<List<TimePeriodUsage>> paginatedManualMonths = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_monitor);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Energy Usage Monitor");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        chartView = findViewById(R.id.energy_bar_chart);
        toggleSwitch = findViewById(R.id.toggle_usage_cost);
        pageIndicatorText = findViewById(R.id.page_indicator);
        summaryText = findViewById(R.id.monthly_summary);
        prevBtn = findViewById(R.id.prev_button);
        nextBtn = findViewById(R.id.next_button);

        pgeDataManager = PgeDataManager.getInstance();
        loadDataBasedOnSource();

        toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> renderCurrentPage());
        nextBtn.setOnClickListener(v -> navigatePage(1));
        prevBtn.setOnClickListener(v -> navigatePage(-1));
    }

    private void loadDataBasedOnSource() {
        currentDataSource = PgeDataManager.getActiveDataSource();
        Log.i("EnergyMonitor", "Loading data for source: " + currentDataSource);

        paginatedApiBills.clear();
        paginatedManualMonths.clear();
        currentPage = 0;

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            List<TimePeriodUsage> manualMonthlyData = pgeDataManager.getManualMonthlyUsage();
            if (manualMonthlyData.isEmpty()) {
                Toast.makeText(this, "No manual data found.", Toast.LENGTH_LONG).show();
                Log.w("EnergyMonitor", "Manual data source active, but no aggregated data available.");
            } else {
                Log.i("EnergyMonitor", "Processing manual monthly data count: " + manualMonthlyData.size());
                Collections.reverse(manualMonthlyData);
                paginatedManualMonths = paginateData(manualMonthlyData, itemsPerPage);
            }
        } else {
            List<BillsResponse.Bill> apiBills = SettingsActivity.getBills();
            if (apiBills == null || apiBills.isEmpty()) {
                Toast.makeText(this, "No API/Demo data available. Connect in Settings.", Toast.LENGTH_LONG).show();
                Log.w("EnergyMonitor", "API data source active, but no bills available.");
            } else {
                Log.i("EnergyMonitor", "Processing API/Demo data count: " + apiBills.size());
                List<BillsResponse.Bill> billsCopy = new ArrayList<>(apiBills);
                Collections.reverse(billsCopy);
                paginatedApiBills = paginateData(billsCopy, itemsPerPage);
            }
        }

        renderCurrentPage();
    }

    private <T> List<List<T>> paginateData(List<T> data, int itemsPerPage) {
        List<List<T>> pages = new ArrayList<>();
        if (data == null || data.isEmpty() || itemsPerPage <= 0) {
            return pages;
        }
        for (int i = 0; i < data.size(); i += itemsPerPage) {
            int end = Math.min(i + itemsPerPage, data.size());
            if (i < end) {
                pages.add(new ArrayList<>(data.subList(i, end)));
            }
        }
        Log.d("EnergyMonitor", "Pagination created " + pages.size() + " pages.");
        return pages;
    }

    private void navigatePage(int direction) {
        int totalPages = getTotalPages();
        if (totalPages <= 0) return;

        int nextPage = currentPage + direction;

        if (nextPage >= 0 && nextPage < totalPages) {
            currentPage = nextPage;
            renderCurrentPage();
        }
    }

    private int getTotalPages() {
        return (currentDataSource == PgeDataManager.DataSource.MANUAL)
                ? paginatedManualMonths.size()
                : paginatedApiBills.size();
    }

    private void renderCurrentPage() {
        boolean showCost = toggleSwitch.isChecked();
        int totalPages = getTotalPages();

        if (totalPages == 0 || currentPage < 0 || currentPage >= totalPages) {
            chartView.setData(new ArrayList<>());
            chartView.setLabels(new ArrayList<>());
            pageIndicatorText.setText("Page 0 of 0");
            summaryText.setText("No data available");
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            Log.w("EnergyMonitor", "No data to render or invalid page.");
            return;
        }

        List<Float> dataValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        float totalValue = 0;
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            List<TimePeriodUsage> currentPageData = paginatedManualMonths.get(currentPage);
            for (TimePeriodUsage monthData : currentPageData) {
                if (monthData == null) continue;
                float value = (float) (showCost ? monthData.getTotalCost() : monthData.getTotalKwh());
                dataValues.add(value);
                totalValue += value;
                String label = "N/A";
                if (monthData.getPeriodStart() != null) {
                    label = monthData.getPeriodStart().format(labelFormatter);
                }
                labels.add(label);
            }
        } else {
            List<BillsResponse.Bill> currentPageData = paginatedApiBills.get(currentPage);
            for (BillsResponse.Bill bill : currentPageData) {
                if (bill == null || bill.getBase() == null) continue;
                float value = (float) (showCost ? bill.getBase().getBillTotalCost() : bill.getBase().getBillTotalKwh());
                dataValues.add(value);
                totalValue += value;
                String label = "N/A";
                String dateString = bill.getBase().getBillStartDate();
                if(dateString != null) {
                    try {
                        LocalDateTime startDate = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
                        label = startDate.format(labelFormatter);
                    } catch (DateTimeParseException e) {
                        label = dateString.substring(0, Math.min(7, dateString.length()));
                        Log.w("EnergyMonitor", "Could not parse API date for label: " + dateString);
                    }
                }
                labels.add(label);
            }
        }

        chartView.setLabels(labels);
        chartView.setData(dataValues);

        pageIndicatorText.setText(String.format(Locale.US, "Page %d of %d", currentPage + 1, totalPages));

        if (dataValues.isEmpty()) {
            summaryText.setText("No data for this period");
        } else {
            float average = totalValue / dataValues.size();
            summaryText.setText(String.format(Locale.US,"Average: %.2f %s", average, showCost ? "$" : "kWh"));
        }

        prevBtn.setEnabled(currentPage > 0);
        nextBtn.setEnabled(currentPage < totalPages - 1);
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
        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
                if (pgeDataManager != null) {
                    pgeDataManager.clearManualData();
                }
                SettingsActivity.getBills().clear();
                PgeDataManager.setActiveDataSource(PgeDataManager.DataSource.API);

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