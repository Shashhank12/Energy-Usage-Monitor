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
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import com.google.android.material.snackbar.Snackbar;

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
    public List<String> notifications = new ArrayList<String>();
    public List<String> snackbarQueue = new ArrayList<>();
    public boolean isSnackbarShowing = false;
    private static int onCreateCount = 0;

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

        String message = (String) outlierText.getText();

        notifications.add("Make sure to check your monthly energy bill!");
        notifications.add(message);
        notifications.add("New Energy Saving Reminder: " + randomTip);

        showQueuedSnackbar("Make sure to check your monthly energy bill!");
        if(onCreateCount==0)
        {
            showQueuedSnackbar(message);
        }
        onCreateCount++;
        showQueuedSnackbar("New Energy Saving Reminder: " + randomTip);

        showLoginNotifications();
    }

    public void showQueuedSnackbar(String message) {
        snackbarQueue.add(message);
        if (!isSnackbarShowing) {
            showNextSnackbar();
        }
    }

    public void showNextSnackbar() {
        if (snackbarQueue.isEmpty()) {
            isSnackbarShowing = false;
            return;
        }

        isSnackbarShowing = true;
        String message = snackbarQueue.remove(0);
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                showNextSnackbar();
            }
        });
        snackbar.show();
    }

    public void showLoginNotifications() {
        for (int i = 0; i < notifications.size(); i++) {
            showNotification(this, notifications.get(i), i);
        }
    }

    public void showNotification(Context context, String message, int notificationId) {
        String channelId = "login_notifications";
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, HomeDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Login Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Energy usage alerts shown on login.");
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Notice")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        notificationManager.notify(notificationId, builder.build());
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

    public void processEnergyData(double userBudget) {
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

    public void updateTrendsFromManual(List<TimePeriodUsage> manualMonths) {
        if (manualMonths.size() < 2) return;
        TimePeriodUsage latestMonth = manualMonths.get(manualMonths.size() - 1);
        TimePeriodUsage previousMonth = manualMonths.get(manualMonths.size() - 2);

        double usagePercent = calculatePercentageChange(latestMonth.getTotalKwh(), previousMonth.getTotalKwh());
        double costPercent = calculatePercentageChange(latestMonth.getTotalCost(), previousMonth.getTotalCost());

        updateTrendTextViews(usagePercent, costPercent);
    }

    public void updateTrendsFromApi(List<BillsResponse.Bill> apiBills) {
        if (apiBills.size() < 2) return;
        BillsResponse.Bill latestBill = apiBills.get(0);
        BillsResponse.Bill previousBill = apiBills.get(1);

        if (latestBill.getBase() == null || previousBill.getBase() == null) return;

        double usagePercent = calculatePercentageChange(latestBill.getBase().getBillTotalKwh(), previousBill.getBase().getBillTotalKwh());
        double costPercent = calculatePercentageChange(latestBill.getBase().getBillTotalCost(), previousBill.getBase().getBillTotalCost());

        updateTrendTextViews(usagePercent, costPercent);
    }

    public double calculatePercentageChange(double currentValue, double previousValue) {
        if (previousValue == 0) {
            return (currentValue == 0) ? 0 : (currentValue > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        return ((currentValue - previousValue) / previousValue) * 100.0;
    }

    public void updateTrendTextViews(double usagePercent, double costPercent) {
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


    public void updateOutlierFromManual(List<TimePeriodUsage> manualMonths) {
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
        String message = "";
        if (olderAvg == 0 && recentAvg > 0) {
            message = "Monthly cost trend has started from zero.";
            outlierText.setText(message);
        } else if (recentAvg < olderAvg) {
            message = "Your average monthly cost is trending upward. Consult Energy Analysis for advice on better savings!";
            outlierText.setText(message);
        } else {
            message = "Your average monthly cost is trending downward. Congratulations! Consult Energy Analysis to save even more!";
            outlierText.setText(message);
        }

        notifications.set(1, message);
        showNotification(this, notifications.get(1), 1);

        showQueuedSnackbar(message);
    }

    public void updatePieChartFromManual(List<TimePeriodUsage> manualMonths, double userBudget) {
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

        Typeface montserrat = ResourcesCompat.getFont(this, R.font.montserrat);
        Typeface montserratBold = ResourcesCompat.getFont(this, R.font.montserrat_bold);

        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(25f, 15f, 25f, 15f);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setCenterTextTypeface(montserratBold);
        pieChart.setCenterTextColor(Color.parseColor("#090909"));
        pieChart.setCenterTextSize(14f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.parseColor("#FAF5F5"));

        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.setDrawCenterText(true);

        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        pieChart.animateY(1400, Easing.EaseInOutQuad);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true);
        l.setXEntrySpace(10f);
        l.setYEntrySpace(5f);
        l.setYOffset(10f);
        l.setEnabled(true);
        l.setTypeface(montserrat);
        l.setTextColor(Color.parseColor("#333333"));
        l.setTextSize(14f);


        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTypeface(montserratBold);
        pieChart.setEntryLabelTextSize(14f);


        if (userBudget <= 0) {
            setupPieChartNoData("Set a budget in Profile!");
            return;
        }

        double budgetRemaining = Math.max(0, userBudget - latestPeriodCost);
        float usedPercentage = (float) (latestPeriodCost / userBudget * 100);
        float remainingPercentage = (float) (budgetRemaining / userBudget * 100);

        ArrayList<Integer> colors = new ArrayList<>();

        String centerText = "";

        if (latestPeriodCost < userBudget) {
            entries.add(new PieEntry(usedPercentage, "Spent"));
            entries.add(new PieEntry(remainingPercentage, "Remaining"));
            colors.add(Color.parseColor("#FFC107"));
            colors.add(Color.parseColor("#4CAF50"));
            centerText = String.format("$%.2f\nRemaining", budgetRemaining);
        } else if (latestPeriodCost == userBudget) {
            entries.add(new PieEntry(100f, "Spent"));
            colors.add(Color.parseColor("#FF9800"));
            centerText = "Budget Met!";
        } else {
            entries.add(new PieEntry(100f, "Budget Limit"));
            colors.add(Color.parseColor("#F44336"));
            centerText = String.format("Over by\n$%.2f!", Math.abs(budgetRemaining));
        }

        pieChart.setCenterText(centerText);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);

        dataSet.setValueLinePart1OffsetPercentage(60.f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.5f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);


        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        data.setValueTypeface(montserrat);

        pieChart.setData(data);

        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

    private void setupPieChartNoData(String message) {
        pieChart.clear();
        pieChart.setCenterText(message);

        Typeface montserrat = ResourcesCompat.getFont(this, R.font.montserrat);
        pieChart.setCenterTextTypeface(montserrat);
        pieChart.setCenterTextColor(Color.parseColor("#A6A6A6"));
        pieChart.setCenterTextSize(14f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.parseColor("#FAF5F5"));

        pieChart.setHighlightPerTapEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);

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
        } else if (id == R.id.nav_energy_monitor) {
            startActivity(new Intent(this, EnergyMonitorActivity.class));
        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
        } else if (id == R.id.nav_household_comparison) {
            startActivity(new Intent(this, HouseholdComparisonActivity.class));
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


    public String getRandomEnergySavingTip() {
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