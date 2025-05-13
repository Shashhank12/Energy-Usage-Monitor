package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.databinding.ActivityHouseholdComparisonBinding;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreCallback;
import edu.sjsu.android.energyusagemonitor.firestore.FirestoreRepository;
import edu.sjsu.android.energyusagemonitor.firestore.HouseholdProfile;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utils.CustomValueFormatter;

public class HouseholdComparisonActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HouseholdComparisonAct";
    private ActivityHouseholdComparisonBinding binding;
    private FirestoreRepository firestoreRepository;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private FirebaseUser currentUser;
    private PgeDataManager pgeDataManager;
    private PgeDataManager.DataSource currentDataSource;
    private double currentUserLastMonthKWh = 0;
    private String userLastMonthLabel = "Your Recent Usage";
    private YearMonth userLastMonthYearMonth;

    private static class ComparisonService {
        private static final double[] CALIFORNIA_AVG_KWH_PER_MONTH_BASE = {
                300, 280, 260, 270, 300, 380, 450, 480, 400, 320, 290, 310
        };
        private static final double KWH_PER_ADDITIONAL_BEDROOM = 75;
        private static final double KWH_PER_ADDITIONAL_OCCUPANT = 50;
        private static final Random random = new Random();

        public static class ComparisonResult {
            public double estimatedAverageKWh;
            public String analysisText;
            public String comparedMonthLabel;
            public ComparisonResult(double estimatedAverageKWh, String analysisText, String comparedMonthLabel) {
                this.estimatedAverageKWh = estimatedAverageKWh;
                this.analysisText = analysisText;
                this.comparedMonthLabel = comparedMonthLabel;
            }
        }

        public static ComparisonResult getEstimatedComparisonData(HouseholdProfile userProfile, double currentUserUsage, YearMonth userUsageMonth) {
            Log.i(TAG, "Calculating estimated average for profile: " + userProfile.getZipCode() +
                    ", Beds: " + userProfile.getBedrooms() + ", Occupants: " + userProfile.getOccupants() +
                    " for month: " + userUsageMonth.toString());
            int monthIndex = userUsageMonth.getMonthValue() - 1;
            double baseMonthlyKWh;
            if (monthIndex >= 0 && monthIndex < CALIFORNIA_AVG_KWH_PER_MONTH_BASE.length) {
                baseMonthlyKWh = CALIFORNIA_AVG_KWH_PER_MONTH_BASE[monthIndex];
            } else {
                double sum = 0;
                for (double val : CALIFORNIA_AVG_KWH_PER_MONTH_BASE) sum += val;
                baseMonthlyKWh = sum / CALIFORNIA_AVG_KWH_PER_MONTH_BASE.length;
                Log.w(TAG, "Month index out of bounds for base CA array, using overall average.");
            }
            if (userProfile.getBedrooms() > 1) {
                baseMonthlyKWh += (userProfile.getBedrooms() - 1) * KWH_PER_ADDITIONAL_BEDROOM;
            }
            if (userProfile.getOccupants() > 1) {
                baseMonthlyKWh += (userProfile.getOccupants() - 1) * KWH_PER_ADDITIONAL_OCCUPANT;
            }
            double climateFactor = 1.0;
            String zip = userProfile.getZipCode();
            if (zip != null && !zip.isEmpty()) {
                try {
                    char firstDigit = zip.charAt(0);
                    if (firstDigit == '9') {
                        if (userUsageMonth.getMonthValue() >= 6 && userUsageMonth.getMonthValue() <= 9) {
                            climateFactor = 1.15;
                        } else if (userUsageMonth.getMonthValue() <=3 || userUsageMonth.getMonthValue() >= 11){
                            climateFactor = 1.05;
                        }
                    } else if (firstDigit == '0' || firstDigit == '1') {
                        climateFactor = 1.10;
                    } else if (firstDigit == '3') {
                        climateFactor = 1.20;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not parse zip for climate adjustment: " + zip);
                }
            }
            double estimatedAverageKWh = baseMonthlyKWh * climateFactor;
            if (estimatedAverageKWh < 100) estimatedAverageKWh = 100;

            String analysis;
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
            String formattedUserUsageMonth = userUsageMonth.format(monthFormatter);
            if (currentUserUsage <= 0) {
                analysis = "Your usage data for " + formattedUserUsageMonth + " is not available or is zero. We can't provide a comparison without it.";
            } else {
                double difference = currentUserUsage - estimatedAverageKWh;
                double percentageDifference = (estimatedAverageKWh > 0) ? (difference / estimatedAverageKWh) * 100 : 0;
                if (percentageDifference > 25) {
                    analysis = String.format(Locale.getDefault(), "For %s, your usage (%.0f kWh) was notably higher (around %.0f%% more) than the estimated average (%.0f kWh) for similar households in your area.", formattedUserUsageMonth, currentUserUsage, percentageDifference, estimatedAverageKWh);
                } else if (percentageDifference < -25) {
                    analysis = String.format(Locale.getDefault(), "Excellent! For %s, your usage (%.0f kWh) was significantly lower (around %.0f%% less) than the estimated average (%.0f kWh) for similar households in your area.", formattedUserUsageMonth, currentUserUsage, Math.abs(percentageDifference), estimatedAverageKWh);
                } else if (percentageDifference > 10) {
                    analysis = String.format(Locale.getDefault(), "For %s, your usage (%.0f kWh) was somewhat higher (around %.0f%% more) than the estimated average (%.0f kWh) for similar households in your area.", formattedUserUsageMonth, currentUserUsage, percentageDifference, estimatedAverageKWh);
                } else if (percentageDifference < -10) {
                    analysis = String.format(Locale.getDefault(), "Good work! For %s, your usage (%.0f kWh) was a bit lower (around %.0f%% less) than the estimated average (%.0f kWh) for similar households in your area.", formattedUserUsageMonth, currentUserUsage, Math.abs(percentageDifference), estimatedAverageKWh);
                } else {
                    analysis = String.format(Locale.getDefault(), "For %s, your usage (%.0f kWh) was relatively close to the estimated average (%.0f kWh) for similar households in your area.", formattedUserUsageMonth, currentUserUsage, estimatedAverageKWh);
                }
            }
            return new ComparisonResult(estimatedAverageKWh, analysis, formattedUserUsageMonth);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHouseholdComparisonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        firestoreRepository = new FirestoreRepository();
        pgeDataManager = PgeDataManager.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            navigateToLogin();
            return;
        }
        setupToolbarAndDrawer();
        fetchCurrentUserLastMonthUsage();
        loadUserProfile();
        binding.btnSaveProfile.setOnClickListener(v -> saveProfileAndTriggerComparison());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HouseholdComparisonActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchCurrentUserLastMonthUsage() {
        currentDataSource = PgeDataManager.getActiveDataSource();
        currentUserLastMonthKWh = 0;
        userLastMonthYearMonth = YearMonth.now().minusMonths(1);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault());
        userLastMonthLabel = userLastMonthYearMonth.format(labelFormatter);

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            List<TimePeriodUsage> manualMonthlyData = pgeDataManager.getManualMonthlyUsage();
            if (manualMonthlyData != null && !manualMonthlyData.isEmpty()) {
                Optional<TimePeriodUsage> targetMonthData = manualMonthlyData.stream()
                        .filter(usage -> usage.getPeriodStart() != null && YearMonth.from(usage.getPeriodStart()).equals(userLastMonthYearMonth))
                        .findFirst();
                if (targetMonthData.isPresent()) {
                    currentUserLastMonthKWh = targetMonthData.get().getTotalKwh();
                } else {
                    manualMonthlyData.stream().max(TimePeriodUsage::compareTo).ifPresent(usage -> {
                        currentUserLastMonthKWh = usage.getTotalKwh();
                        if (usage.getPeriodStart() != null) {
                            userLastMonthYearMonth = YearMonth.from(usage.getPeriodStart());
                            userLastMonthLabel = userLastMonthYearMonth.format(labelFormatter);
                        }
                    });
                }
            }
        } else {
            List<BillsResponse.Bill> apiBills = SettingsActivity.getBills();
            if (apiBills != null && !apiBills.isEmpty()) {
                List<BillsResponse.Bill> sortedBills = new ArrayList<>(apiBills);
                Collections.sort(sortedBills, (b1, b2) -> {
                    try {
                        return LocalDate.parse(b2.getBase().getBillEndDate(), DateTimeFormatter.ISO_DATE_TIME)
                                .compareTo(LocalDate.parse(b1.getBase().getBillEndDate(), DateTimeFormatter.ISO_DATE_TIME));
                    } catch (Exception e) { return 0; }
                });
                Optional<BillsResponse.Bill> targetMonthBill = sortedBills.stream()
                        .filter(bill -> bill.getBase() != null && bill.getBase().getBillStartDate() != null)
                        .filter(bill -> {
                            try {
                                return YearMonth.from(LocalDate.parse(bill.getBase().getBillStartDate(), DateTimeFormatter.ISO_DATE_TIME)).equals(userLastMonthYearMonth);
                            } catch (DateTimeParseException e) { return false; }
                        })
                        .findFirst();
                if (targetMonthBill.isPresent()) {
                    currentUserLastMonthKWh = targetMonthBill.get().getBase().getBillTotalKwh();
                } else if (!sortedBills.isEmpty() && sortedBills.get(0).getBase() != null) {
                    currentUserLastMonthKWh = sortedBills.get(0).getBase().getBillTotalKwh();
                    try {
                        userLastMonthYearMonth = YearMonth.from(LocalDate.parse(sortedBills.get(0).getBase().getBillStartDate(), DateTimeFormatter.ISO_DATE_TIME));
                        userLastMonthLabel = userLastMonthYearMonth.format(labelFormatter);
                    } catch (DateTimeParseException e) { /* keep default */ }
                }
            }
        }
        if (currentUserLastMonthKWh > 0) {
            Log.i(TAG, "User's usage for " + userLastMonthLabel + " (" + userLastMonthYearMonth.toString() + "): " + currentUserLastMonthKWh + " kWh");
        } else {
            Log.w(TAG, "Could not determine user's recent usage.");
            userLastMonthLabel = "Your Recent Usage";
        }
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbarComparison);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayoutComparison, binding.toolbarComparison,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayoutComparison.addDrawerListener(toggle);
        toggle.syncState();
        binding.navViewComparison.setNavigationItemSelectedListener(this);
        try {
            binding.navViewComparison.setCheckedItem(R.id.nav_household_comparison);
        } catch (Exception e) {
            Log.e(TAG, "Menu item R.id.nav_household_comparison not found.", e);
        }
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        firestoreRepository.getDocumentById("householdProfiles", currentUserId, HouseholdProfile.class, new FirestoreCallback<HouseholdProfile>() {
            @Override
            public void onSuccess(HouseholdProfile profile) {
                if (profile != null) {
                    binding.etBedrooms.setText(String.valueOf(profile.getBedrooms()));
                    binding.etOccupants.setText(String.valueOf(profile.getOccupants()));
                    binding.etZipCode.setText(profile.getZipCode());
                    performComparison(profile);
                } else {
                    binding.cardComparisonResult.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading household profile", e);
                Toast.makeText(HouseholdComparisonActivity.this, "Error loading profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileAndTriggerComparison() {
        String bedroomsStr = binding.etBedrooms.getText().toString().trim();
        String occupantsStr = binding.etOccupants.getText().toString().trim();
        String zipCodeStr = binding.etZipCode.getText().toString().trim();
        if (!validateProfileInput(bedroomsStr, occupantsStr, zipCodeStr)) return;
        try {
            int bedrooms = Integer.parseInt(bedroomsStr);
            int occupants = Integer.parseInt(occupantsStr);
            HouseholdProfile profile = new HouseholdProfile(bedrooms, occupants, zipCodeStr);
            firestoreRepository.addDocument("householdProfiles", currentUserId, profile, new FirestoreCallback<String>() {
                @Override
                public void onSuccess(String documentId) {
                    Toast.makeText(HouseholdComparisonActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    performComparison(profile);
                }
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error saving profile", e);
                    Toast.makeText(HouseholdComparisonActivity.this, "Error saving profile.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for bedrooms and occupants.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateProfileInput(String bedrooms, String occupants, String zip) {
        if (TextUtils.isEmpty(bedrooms) || TextUtils.isEmpty(occupants) || TextUtils.isEmpty(zip)) {
            Toast.makeText(this, "Please fill all profile fields.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (zip.length() != 5) {
            binding.tilZipCode.setError("Zip code must be 5 digits");
            return false;
        } else {
            binding.tilZipCode.setError(null);
        }
        try {
            if (Integer.parseInt(bedrooms) <= 0 || Integer.parseInt(occupants) <= 0) {
                Toast.makeText(this, "Bedrooms and occupants must be greater than zero.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format for bedrooms or occupants.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void performComparison(HouseholdProfile userProfile) {
        if (currentUserLastMonthKWh <= 0 || userLastMonthYearMonth == null) {
            binding.cardComparisonResult.setVisibility(View.VISIBLE);
            binding.tvComparisonIntro.setText(String.format(Locale.getDefault(),
                    "Profile: Zip %s, %d Beds, %d Occupants",
                    userProfile.getZipCode(), userProfile.getBedrooms(), userProfile.getOccupants()));
            binding.tvComparisonIntro.setVisibility(View.VISIBLE);
            binding.tvComparisonDetails.setText("Your recent energy usage data is not available. Please ensure your data source is connected and up-to-date in Settings to see a comparison.");
            binding.tvComparisonDetails.setVisibility(View.VISIBLE);
            binding.barChartComparison.setVisibility(View.GONE);
            return;
        }
        ComparisonService.ComparisonResult result =
                ComparisonService.getEstimatedComparisonData(userProfile, currentUserLastMonthKWh, userLastMonthYearMonth);
        binding.cardComparisonResult.setVisibility(View.VISIBLE);
        binding.tvComparisonDetails.setText(result.analysisText);
        String introBase = String.format(Locale.getDefault(),
                "Your usage for %s: %.0f kWh. This is compared against an estimated average calculated with regards to your household's bedrooms, occupants, and zip code for %s.",
                userLastMonthLabel, currentUserLastMonthKWh, result.comparedMonthLabel);
        binding.tvComparisonIntro.setText(introBase);
        binding.tvComparisonIntro.setVisibility(View.VISIBLE);
        if (result.estimatedAverageKWh > 0) {
            binding.barChartComparison.setVisibility(View.VISIBLE);
            setupBarChart(currentUserLastMonthKWh, result.estimatedAverageKWh, userLastMonthLabel, "Est. Similar Avg.");
        } else {
            binding.barChartComparison.setVisibility(View.GONE);
        }
    }

    private void setupBarChart(double userUsage, double comparisonAverage, String userUsageLabel, String comparisonLabel) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) userUsage));
        entries.add(new BarEntry(1, (float) comparisonAverage));
        BarDataSet dataSet = new BarDataSet(entries, "Energy Usage (kWh)");
        int colorYourUsage = getResources().getColor(R.color.your_usage_color, getTheme());
        int colorAverageUsage = getResources().getColor(R.color.average_usage_color, getTheme());
        dataSet.setColors(new int[]{colorYourUsage, colorAverageUsage});
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new CustomValueFormatter());
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        binding.barChartComparison.setData(barData);
        binding.barChartComparison.getDescription().setEnabled(false);
        binding.barChartComparison.setDrawGridBackground(false);
        binding.barChartComparison.animateY(1000);
        binding.barChartComparison.setFitBars(true);
        XAxis xAxis = binding.barChartComparison.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{userUsageLabel, comparisonLabel}));
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(0);
        xAxis.setYOffset(5f);
        YAxis leftAxis = binding.barChartComparison.getAxisLeft();
        leftAxis.setLabelCount(6, false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(20f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setTextSize(11f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#e0e0e0"));
        binding.barChartComparison.getAxisRight().setEnabled(false);
        binding.barChartComparison.getLegend().setEnabled(false);
        binding.barChartComparison.setExtraOffsets(5f, 10f, 5f, 15f);
        binding.barChartComparison.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_home_dashboard) {
            intent = new Intent(this, HomeDashboardActivity.class);
        } else if (id == R.id.nav_energy_monitor) {
            intent = new Intent(this, EnergyMonitorActivity.class);
        } else if (id == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (id == R.id.nav_analysis) {
            intent = new Intent(this, AnalysisActivity.class);
        } else if (id == R.id.nav_household_comparison) {
            binding.drawerLayoutComparison.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_logout) {
            logoutUser();
            return true;
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        binding.drawerLayoutComparison.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());
        if (pgeDataManager != null) {
            pgeDataManager.clearAllData();
        }
        if (SettingsActivity.getBills() != null) {
            SettingsActivity.getBills().clear();
        }
        PgeDataManager.setActiveDataSource(PgeDataManager.DataSource.API);
        mAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            navigateToLogin();
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayoutComparison.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayoutComparison.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}