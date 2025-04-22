package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreCallback;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreRepository;
import edu.sjsu.android.energyusagemonitor.firestore.users;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

public class HomeDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HomeDashboardActivity";

    private DrawerLayout drawerLayout;
    private FirestoreRepository firestoreRepository;
    private PgeDataManager pgeDataManager;
    private double budget = 0;

    private PieChart pieChart;
    private List<PieEntry> entries = new ArrayList<>();
    private TextView energyUseText;
    private TextView energyCostText;
    private TextView outlierText;
    private TextView energySavingTip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        firestoreRepository = new FirestoreRepository();
        pgeDataManager = PgeDataManager.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Dashboard");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        energyUseText = findViewById(R.id.text_energy_use);
        energyCostText = findViewById(R.id.text_energy_cost);
        outlierText = findViewById(R.id.monthly_trend);
        energySavingTip = findViewById(R.id.random_tip);
        pieChart = findViewById(R.id.pieChart);

        String randomTip = getRandomEnergySavingTip();
        energySavingTip.setText("New Energy Saving Reminder: " + randomTip);

        fetchUserBudgetAndProcessData();
    }

    private void fetchUserBudgetAndProcessData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in.");
            handleNoDataAvailable("Login required to view data.");
            return;
        }
        String userId = currentUser.getUid();

        firestoreRepository.getDocumentById("users", userId, users.class, new FirestoreCallback<users>() {
            @Override
            public void onSuccess(users userData) {
                Log.d(TAG, "User data fetched successfully for budget.");
                try {
                    String budgetStr = userData.getBudget();
                    if (budgetStr == null || budgetStr.trim().isEmpty()) {
                        throw new NullPointerException("Budget string is null or empty");
                    }
                    budget = Double.parseDouble(budgetStr.replace(",", ""));

                    processEnergyData(budget);

                } catch (NumberFormatException | NullPointerException e) {
                    Log.e(TAG, "Error parsing budget: " + e.getMessage());
                    budget = 0;
                    processEnergyData(budget);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch user data: " + e.getMessage());
                handleNoDataAvailable("Could not load user profile.");
            }
        });
    }

    private void processEnergyData(double userBudget) {
        PgeDataManager.DataSource activeSource = PgeDataManager.getActiveDataSource();
        Log.i(TAG, "Processing energy data for source: " + activeSource);

        if (activeSource == PgeDataManager.DataSource.MANUAL) {
            List<TimePeriodUsage> manualMonths = pgeDataManager.getManualMonthlyUsage();
            if (manualMonths != null && manualMonths.size() >= 2) {
                updateTrendsFromManual(manualMonths);
                updateOutlierFromManual(manualMonths);
                updatePieChartFromManual(manualMonths, userBudget);
            } else {
                Log.w(TAG, "Insufficient manual data for trends/pie chart.");
                handleNoDataAvailable("Not enough manual data uploaded.");
            }
        } else {
            List<BillsResponse.Bill> apiBills = SettingsActivity.getBills();
            if (apiBills != null && apiBills.size() >= 2) {
                updateTrendsFromApi(apiBills);
                updateOutlierFromApi(apiBills);
                updatePieChartFromApi(apiBills, userBudget);
            } else {
                Log.w(TAG, "Insufficient API/Demo data for trends/pie chart.");
                handleNoDataAvailable("Connect to Utility API or upload data in Settings.");
            }
        }
    }

    private void updateTrendsFromManual(List<TimePeriodUsage> manualMonths) {
        if (manualMonths.size() < 2) return;
        TimePeriodUsage latestMonth = manualMonths.get(manualMonths.size() - 1);
        TimePeriodUsage previousMonth = manualMonths.get(manualMonths.size() - 2);

        double usagePercent = calculatePercentageChange(latestMonth.getTotalKwh(), previousMonth.getTotalKwh());
        double costPercent = calculatePercentageChange(latestMonth.getTotalCost(), previousMonth.getTotalCost());

        updateTrendTextViews(usagePercent, costPercent);
    }

    private void updateTrendsFromApi(List<BillsResponse.Bill> apiBills) {
        if (apiBills.size() < 2) return;
        BillsResponse.Bill latestBill = apiBills.get(0);
        BillsResponse.Bill previousBill = apiBills.get(1);

        if (latestBill.getBase() == null || previousBill.getBase() == null) return;

        double usagePercent = calculatePercentageChange(latestBill.getBase().getBillTotalKwh(), previousBill.getBase().getBillTotalKwh());
        double costPercent = calculatePercentageChange(latestBill.getBase().getBillTotalCost(), previousBill.getBase().getBillTotalCost());

        updateTrendTextViews(usagePercent, costPercent);
    }

    private double calculatePercentageChange(double currentValue, double previousValue) {
        if (previousValue == 0) {
            return (currentValue == 0) ? 0 : (currentValue > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        return ((currentValue - previousValue) / previousValue) * 100.0;
    }

    private void updateTrendTextViews(double usagePercent, double costPercent) {
        String energyUseTextStr;
        if (Double.isInfinite(usagePercent)) energyUseTextStr = "Monthly energy use changed from zero.";
        else if (usagePercent > 0) energyUseTextStr = "Monthly energy use is up " + String.format("%.1f", usagePercent) + "%.";
        else if (usagePercent < 0) energyUseTextStr = "Monthly energy use is down " + String.format("%.1f", Math.abs(usagePercent)) + "%.";
        else energyUseTextStr = "Monthly energy use stayed the same.";

        String energyCostTextStr;
        if (Double.isInfinite(costPercent)) energyCostTextStr = "Monthly energy cost changed from zero.";
        else if (costPercent > 0) energyCostTextStr = "Monthly energy cost is up " + String.format("%.1f", costPercent) + "%.";
        else if (costPercent < 0) energyCostTextStr = "Monthly energy cost is down " + String.format("%.1f", Math.abs(costPercent)) + "%.";
        else energyCostTextStr = "Monthly energy cost stayed the same.";

        energyUseText.setText(energyUseTextStr);
        energyCostText.setText(energyCostTextStr);
    }


    private void updateOutlierFromManual(List<TimePeriodUsage> manualMonths) {
        if (manualMonths.size() < 12) {
            outlierText.setText("Need at least 12 months of data for trend analysis.");
            return;
        }

        double firstHalfAvg = manualMonths.stream().skip(manualMonths.size() - 12).limit(6)
                .mapToDouble(TimePeriodUsage::getTotalCost).average().orElse(0);
        double secondHalfAvg = manualMonths.stream().skip(manualMonths.size() - 6)
                .mapToDouble(TimePeriodUsage::getTotalCost).average().orElse(0);

        updateOutlierTextView(firstHalfAvg, secondHalfAvg);
    }

    private void updateOutlierFromApi(List<BillsResponse.Bill> apiBills) {
        if (apiBills.size() < 12) {
            outlierText.setText("Need at least 12 months of data for trend analysis.");
            return;
        }

        double firstHalfAvg = apiBills.stream().limit(6)
                .filter(b -> b != null && b.getBase() != null)
                .mapToDouble(b -> b.getBase().getBillTotalCost()).average().orElse(0);
        double secondHalfAvg = apiBills.stream().skip(6).limit(6)
                .filter(b -> b != null && b.getBase() != null)
                .mapToDouble(b -> b.getBase().getBillTotalCost()).average().orElse(0);

        updateOutlierTextView(firstHalfAvg, secondHalfAvg);
    }

    private void updateOutlierTextView(double recentAvg, double olderAvg) {
        if (olderAvg == 0 && recentAvg > 0) {
            outlierText.setText("Monthly cost trend has started from zero.");
        } else if (recentAvg < olderAvg) {
            outlierText.setText("Your average monthly cost is trending upward. Consult Energy Analysis for advice on better savings!");
        } else {
            outlierText.setText("Your average monthly cost is trending downward. Congratulations! Consult Energy Analysis to save even more!");
        }
    }


    private void updatePieChartFromManual(List<TimePeriodUsage> manualMonths, double userBudget) {
        if (manualMonths.isEmpty()) {
            setupPieChartNoData("No manual data available.");
            return;
        }
        TimePeriodUsage latestMonth = manualMonths.get(manualMonths.size() - 1);
        setupPieChart(latestMonth.getTotalCost(), userBudget);
    }

    private void updatePieChartFromApi(List<BillsResponse.Bill> apiBills, double userBudget) {
        if (apiBills.isEmpty()) {
            setupPieChartNoData("No API/Demo data available.");
            return;
        }

        BillsResponse.Bill latestBill = apiBills.get(0);
        if (latestBill.getBase() == null) {
            setupPieChartNoData("Latest bill data is invalid.");
            return;
        }
        setupPieChart(latestBill.getBase().getBillTotalCost(), userBudget);
    }


    private void setupPieChart(double latestPeriodCost, double userBudget) {
        pieChart.clear();
        entries.clear();

        if (userBudget <= 0) {
            pieChart.setNoDataText("Set a valid budget in Profile to see the chart!");
            pieChart.setNoDataTextColor(Color.BLACK);
            pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
            pieChart.invalidate();
            return;
        }

        double budgetRemaining = userBudget - latestPeriodCost;
        float usedPercentage = Math.max(0f, (float) (latestPeriodCost / userBudget * 100));
        float remainingPercentage = Math.max(0f, (float) (budgetRemaining / userBudget * 100));
        List<Integer> colors = new ArrayList<>();

        Description description = new Description();
        description.setEnabled(false);

        if (latestPeriodCost < userBudget) {
            entries.add(new PieEntry(usedPercentage, "Budget Used (%)"));
            entries.add(new PieEntry(remainingPercentage, "Budget Remaining (%)"));
            colors.add(Color.YELLOW);
            colors.add(Color.GREEN);
        } else {
            entries.add(new PieEntry(usedPercentage, "Budget Used (%)"));
            colors.add(Color.RED);

            description.setEnabled(true);
            description.setTextSize(14f);
            description.setTextColor(Color.BLACK);
            if (latestPeriodCost == userBudget) {
                description.setText("Budget used fully!");
            } else {
                description.setText("You've overspent!");
            }
            pieChart.setDescription(description);
        }

        PieDataSet pieDataSet = new PieDataSet(entries, "");
        pieDataSet.setColors(colors);
        pieDataSet.setValueTextSize(16f);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        PieData pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText("Budget vs Spending");
        pieChart.setCenterTextSize(12f);
        pieChart.invalidate();
    }

    private void setupPieChartNoData(String message) {
        pieChart.clear();
        pieChart.setNoDataText(message);
        pieChart.setNoDataTextColor(Color.BLACK);
        pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
        pieChart.invalidate();
    }

    private void handleNoDataAvailable(String message) {
        runOnUiThread(() -> {
            energyUseText.setText("Energy use data unavailable.");
            energyCostText.setText("Energy cost data unavailable.");
            outlierText.setText("Trend analysis requires more data.");
            setupPieChartNoData(message);
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
        } else if (id == R.id.nav_energy_monitor) {
            startActivity(new Intent(this, EnergyMonitorActivity.class));
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


    private String getRandomEnergySavingTip() {
        String[] tips = {
                "Turn off lights when leaving a room.",
                "Unplug chargers when not in use.",
                "Use LED light bulbs.",
                "Set your thermostat a few degrees lower in winter.",
                "Wash clothes in cold water.",
                "Use natural light during the day.",
                "Seal windows and doors to prevent drafts.",
                "Use a programmable thermostat.",
                "Only run full loads in dishwasher and laundry.",
                "Air-dry clothes when possible."
        };
        Random random = new Random();
        int index = random.nextInt(tips.length);
        return tips[index];
    }
}