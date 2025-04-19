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

import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.*;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreCallback;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreRepository;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreSimpleCallback;
import edu.sjsu.android.energyusagemonitor.firestore.TestUser;
import edu.sjsu.android.energyusagemonitor.firestore.users;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.Description;

public class HomeDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirestoreRepository firestoreRepository;
    public List<BillsResponse.Bill> bills;
    private users currentUser;

    private double budget;

    private PieChart pieChart;

    private ArrayList<PieEntry> entries = new ArrayList<>();

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

        if(SettingsActivity.getBills().size()>0){
            bills = SettingsActivity.getBills();

            TextView energyUseText = findViewById(R.id.text_energy_use);
            TextView energyCostText = findViewById(R.id.text_energy_cost);
            TextView outlierText = findViewById(R.id.monthly_trend);

            double energyUse = getEnergyUsePercentage();
            double energyCost = getEnergyCostPercentage();
            boolean isOutlier = isOutlier();

            String energyUseTextStr;
            if (energyUse > 0) {
                energyUseTextStr = "Monthly energy use is up " + String.format("%.2f", energyUse) + "%.";
            } else if (energyUse < 0) {
                energyUseTextStr = "Monthly energy use is down " + String.format("%.2f", Math.abs(energyUse)) + "%.";
            } else {
                energyUseTextStr = "Monthly energy use stayed the same.";
            }

            String energyCostTextStr;
            if (energyCost > 0) {
                energyCostTextStr = "Monthly energy cost is up " + String.format("%.2f", energyCost) + "%.";
            } else if (energyCost < 0) {
                energyCostTextStr = "Monthly energy cost is down " + String.format("%.2f", Math.abs(energyCost)) + "%.";
            } else {
                energyCostTextStr = "Monthly energy cost stayed the same.";
            }

            energyUseText.setText(energyUseTextStr);
            energyCostText.setText(energyCostTextStr);

            if(isOutlier)
            {
                outlierText.setText("Your average monthly usage is trending upward. Consult Energy Analysis for advice on better savings!");
            }
            else
            {
                outlierText.setText("Your average monthly usage is trending downward. Congratulations! Consult Energy Analysis to save even more!");
            }
        }

        TextView energySavingTip = findViewById(R.id.random_tip);
        String randomTip = getRandomEnergySavingTip();
        energySavingTip.setText("New Energy Saving Reminder: " + randomTip);

        pieChart = findViewById(R.id.pieChart);
        getTestUser();
    }

    private void createTestUser()
    {
        users user = new users(FirebaseAuth.getInstance().getCurrentUser().getEmail(), "TestFirstName", "TestLastName", "TestBudget");

        firestoreRepository.addDocument("users", FirebaseAuth.getInstance().getCurrentUser().getUid(), user, new FirestoreCallback<String>() {
            @Override
            public void onSuccess(String documentId) {}

            @Override
            public void onFailure(Exception e) {}
        });
    }

    private void getTestUser() {
        firestoreRepository.getDocumentById("users", FirebaseAuth.getInstance().getCurrentUser().getUid(), users.class, new FirestoreCallback<users>() {
            @Override
            public void onSuccess(users testUser)
            {
                Log.wtf("TestUser", "TestUser: " + testUser.getFirstName());

                try
                {
                    String budgetStr = testUser.getBudget();
                    if (budgetStr == null) throw new NullPointerException("Budget string is null");

                    budgetStr = budgetStr.replace(",", "");
                    budget = Double.parseDouble(budgetStr);

                    if(budget==0)
                    {
                        pieChart.clear();
                        pieChart.setNoDataText("Your Budget in Profile is 0. Increase value to see the chart!");
                        pieChart.setNoDataTextColor(Color.BLACK);
                        pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
                    }

                    else
                    {
                        if(SettingsActivity.getBills().size()>0)
                        {
                            bills = SettingsActivity.getBills();
                            BillsResponse.Bill firstBill = bills.get(0);
                            double firstBillTotalCost = firstBill.getBase().getBillTotalCost();
                            double budgetRemaining = budget - firstBillTotalCost;
                            float usedPercentage = (float) (firstBillTotalCost / budget * 100);
                            float remainingPercentage = (float) (budgetRemaining / budget * 100);
                            List<Integer> colors = new ArrayList<>();

                            if (firstBillTotalCost < budget)
                            {
                                entries.add(new PieEntry(usedPercentage, "Budget Used (%)"));
                                entries.add(new PieEntry(remainingPercentage, "Budget Remaining (%)"));
                                pieChart.setEntryLabelColor(Color.BLACK);

                                colors.add(Color.YELLOW);       // Color for first slice
                                colors.add(Color.GREEN);
                            }
                            else
                            {
                                entries.add(new PieEntry(usedPercentage, "Budget Used (%)"));
                                pieChart.setEntryLabelColor(Color.BLACK);

                                colors.add(Color.YELLOW);
                            }

                            PieDataSet pieDataSet = new PieDataSet(entries, "");
                            pieDataSet.setColors(colors);
                            pieDataSet.setValueTextSize(16f);
                            pieDataSet.setValueTextColor(Color.BLACK);

                            PieData pieData = new PieData(pieDataSet);

                            pieChart.clear();
                            pieChart.setData(pieData);
                            pieChart.setUsePercentValues(true);
                            if(firstBillTotalCost<budget)
                            {
                                pieChart.getDescription().setEnabled(false);
                            }
                            else
                            {
                                Description description = new Description();

                                if(firstBillTotalCost==budget) description.setText("Budget used fully!");
                                else description.setText("You've overspent!");

                                description.setTextSize(14f);
                                description.setTextColor(Color.BLACK);
                                pieChart.setDescription(description);
                            }
                            pieChart.invalidate();
                        }
                        else
                        {
                            pieChart.clear();
                            pieChart.setNoDataText("Your Budget is valid! Connect with Provider in Settings for chart!");
                            pieChart.setNoDataTextColor(Color.BLACK);
                            pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
                        }
                    }
                }
                catch (NumberFormatException | NullPointerException e)
                {
                    pieChart.clear();
                    pieChart.setNoDataText("Make sure Budget in Profile is a valid value to see the chart!");
                    pieChart.setNoDataTextColor(Color.BLACK);
                    pieChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);
                    budget = 0; // Fallback value
                }
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

    private double getEnergyUsePercentage()
    {
        BillsResponse.Bill firstBill = bills.get(0);
        BillsResponse.Bill secondBill = bills.get(1);

        double firstBillTotalKwh = firstBill.getBase().getBillTotalKwh();
        double secondBillTotalKwh = secondBill.getBase().getBillTotalKwh();

        if (secondBillTotalKwh == 0) {
            return (firstBillTotalKwh == 0) ? 0 : (firstBillTotalKwh > 0 ? 100 : -100); // Handle division by zero (if second bill is 0)
        }

        double percentChange = ((firstBillTotalKwh - secondBillTotalKwh) / secondBillTotalKwh) * 100;

        return percentChange;
    }

    private double getEnergyCostPercentage()
    {
        BillsResponse.Bill firstBill = bills.get(0);
        BillsResponse.Bill secondBill = bills.get(1);

        double firstBillTotalCost = firstBill.getBase().getBillTotalCost();
        double secondBillTotalCost = secondBill.getBase().getBillTotalCost();

        if (secondBillTotalCost == 0) {
            return (firstBillTotalCost == 0) ? 0 : (firstBillTotalCost > 0 ? 100 : -100); // Handle division by zero (if second bill is 0)
        }

        double percentChange = ((firstBillTotalCost - secondBillTotalCost) / secondBillTotalCost) * 100;

        return percentChange;
    }

    private boolean isOutlier()
    {
        double firstHalfAvg = IntStream.range(0, 6)
                .mapToDouble(i -> bills.get(i).getBase().getBillTotalCost())
                .average().orElse(0);

        double secondHalfAvg = IntStream.range(6, 12)
                .mapToDouble(i -> bills.get(i).getBase().getBillTotalCost())
                .average().orElse(0);

        return secondHalfAvg < firstHalfAvg;
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
