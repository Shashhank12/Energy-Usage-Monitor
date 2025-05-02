package edu.sjsu.android.energyusagemonitor.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.List;
import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.databinding.ActivitySettingsBinding;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.RetrofitClient;
import edu.sjsu.android.energyusagemonitor.utilityapi.UtilityApiService;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SettingsActivity";
    private UtilityApiService apiService;
    private static List<BillsResponse.Bill> bills = new ArrayList<>();
    private ActivitySettingsBinding binding;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private PgeDataManager pgeDataManager;
    private TextView tvUploadStatus;
    private TextView tvUploadAnalysis;
    private Button btnUploadPgeZip;
    private ImageButton btnShowPgeGuide;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Data Source Settings");

        toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        apiService = RetrofitClient.getClient().create(UtilityApiService.class);

        pgeDataManager = PgeDataManager.getInstance();
        btnUploadPgeZip = binding.btnUploadPgeZip;
        tvUploadStatus = binding.tvUploadStatus;
        tvUploadAnalysis = binding.tvUploadAnalysis;
        btnShowPgeGuide = binding.btnShowPgeGuide;

        initializeFilePickerLauncher();

        btnUploadPgeZip.setOnClickListener(v -> openPgeFilePicker());

        btnShowPgeGuide.setOnClickListener(v -> showPgeUploadGuide());

        binding.connectUtilityButton.setOnClickListener(v -> {
            String meterUid;
            if (Constants.USE_TEST_DATA) {
                meterUid = Constants.TEST_METER_UID;
                Log.d(TAG, "Using test data:");
                Log.d(TAG, "Meter UID: " + meterUid);
            } else {
                meterUid = "YOUR_REAL_METER_ID";
                Log.w(TAG, "Using placeholder for real meter ID");
            }
            tvUploadStatus.setText("Manual upload status");
            tvUploadAnalysis.setVisibility(View.GONE);
            fetchBillsAndSetPreference(meterUid);
        });

        tvUploadStatus.setText("Manual upload status");
    }

    private void initializeFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Log.d(TAG, "Manual upload file selected: " + uri.toString());
                            tvUploadStatus.setText("Processing file...");
                            tvUploadAnalysis.setVisibility(View.GONE);

                            pgeDataManager.loadDataFromZip(this, uri,
                                    () -> {
                                        PgeDataManager.setActiveDataSource(PgeDataManager.DataSource.MANUAL);
                                        runOnUiThread(() -> {
                                            tvUploadStatus.setText("File processed successfully!");
                                            Toast.makeText(this, "Manual PG&E Data Loaded!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(SettingsActivity.this, HomeDashboardActivity.class);
                                            startActivity(intent);
                                        });
                                    },
                                    () -> runOnUiThread(() -> {
                                        tvUploadStatus.setText("Error processing file.");
                                        Toast.makeText(this, "Failed to load manual data.", Toast.LENGTH_LONG).show();
                                        tvUploadAnalysis.setVisibility(View.GONE);
                                    })
                            );
                        } else {
                            tvUploadStatus.setText("Failed to get file URI.");
                            tvUploadAnalysis.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "File selection cancelled or failed.");
                        tvUploadStatus.setText("File selection cancelled.");
                        tvUploadAnalysis.setVisibility(View.GONE);
                    }
                });
    }

    private void openPgeFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String[] mimeTypes = {
                "application/zip",
                "application/x-zip-compressed"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void showPgeUploadGuide() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.pge_guide, null);

        builder.setView(dialogView)
                .setTitle("How to Upload PG&E Data");

        AlertDialog dialog = builder.create();

        Button closeButton = dialogView.findViewById(R.id.button_close_guide);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void fetchBillsAndSetPreference(String meterUid) {
        String apiToken = "Bearer " + Constants.API_TOKEN;
        Call<BillsResponse> fetchBillsCall = apiService.getBills(apiToken, meterUid);
        fetchBillsCall.enqueue(new Callback<BillsResponse>() {
            @Override
            public void onResponse(@NonNull Call<BillsResponse> call, @NonNull Response<BillsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SettingsActivity.bills = response.body().getBills();
                    Log.d(TAG, "Bills fetched successfully via API");
                    logBillData();
                    PgeDataManager.setActiveDataSource(PgeDataManager.DataSource.API);
                    Intent intent = new Intent(SettingsActivity.this, HomeDashboardActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "Failed to fetch bills: " + response.code() + " - " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) { /* Ignore */ }
                    Toast.makeText(SettingsActivity.this, "Failed to connect via API: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BillsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage(), t);
                Toast.makeText(SettingsActivity.this, "API connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
        } else if (id == R.id.nav_home_dashboard) {
            startActivity(new Intent(this, HomeDashboardActivity.class));
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
                bills.clear();
                PgeDataManager.setActiveDataSource(PgeDataManager.DataSource.API);
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
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

    private void logBillData() {
        Log.d(TAG, "--- Logging API Bill Data ---");
        if (bills == null || bills.isEmpty()) {
            Log.d(TAG, "No API bills data to log.");
            return;
        }
        for (BillsResponse.Bill bill : bills) {
            if (bill != null && bill.getBase() != null) {
                Log.d(TAG, "Bill Start Date: " + bill.getBase().getBillStartDate());
                Log.d(TAG, "Bill End Date: " + bill.getBase().getBillEndDate());
                Log.d(TAG, "Total Cost: " + bill.getBase().getBillTotalCost());
                Log.d(TAG, "Total kWh: " + bill.getBase().getBillTotalKwh());
            } else {
                Log.w(TAG, "Encountered null bill or bill base data.");
            }
        }
        Log.d(TAG, "--- End Logging API Bill Data ---");
    }

    public static List<BillsResponse.Bill> getBills() {
        return bills;
    }

    public double getTotalCost() {
        double totalCost = 0;
        if (bills == null) return 0.0;
        for (BillsResponse.Bill bill : bills) {
            if (bill != null && bill.getBase() != null) {
                totalCost += bill.getBase().getBillTotalCost();
            }
        }
        return totalCost;
    }

    public double getTotalUsage() {
        double totalUsage = 0;
        if (bills == null) return 0.0;
        for (BillsResponse.Bill bill : bills) {
            if (bill != null && bill.getBase() != null) {
                totalUsage += bill.getBase().getBillTotalKwh();
            }
        }
        return totalUsage;
    }

    public String getDateRange() {
        if (bills == null || bills.isEmpty()) {
            return "No bills available";
        }
        String startDate = (bills.get(0).getBase() != null && bills.get(0).getBase().getBillStartDate() != null)
                ? bills.get(0).getBase().getBillStartDate() : "N/A";
        String endDate = (bills.get(bills.size() - 1).getBase() != null && bills.get(bills.size() - 1).getBase().getBillEndDate() != null)
                ? bills.get(bills.size() - 1).getBase().getBillEndDate() : "N/A";

        return "Date Range: " + startDate + " to " + endDate;
    }
}