package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.core.view.GravityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Settings Page");

        toggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        apiService = RetrofitClient.getClient().create(UtilityApiService.class);

        binding.connectUtilityButton.setOnClickListener(v -> {
            String meterUid;
            if (Constants.USE_TEST_DATA) {
                meterUid = Constants.TEST_METER_UID;
                Log.d(TAG, "Using test data:");
                Log.d(TAG, "Meter UID: " + meterUid);
            } else {
                meterUid = "REAL_METER";
            }
            fetchBillsAndNavigate(meterUid);
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
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
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

    private void fetchBillsAndNavigate(String meterUid) {
        String apiToken = "Bearer " + Constants.API_TOKEN;
        Call<BillsResponse> fetchBillsCall = apiService.getBills(apiToken, meterUid);
        fetchBillsCall.enqueue(new Callback<BillsResponse>() {
            @Override
            public void onResponse(Call<BillsResponse> call, Response<BillsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bills = response.body().getBills();
                    Log.d(TAG, "Bills fetched successfully");
                    Log.d(TAG, "API Response: " + new Gson().toJson(response.body()));
                    logBillData();

                    Intent intent = new Intent(SettingsActivity.this, EnergyMonitorActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "Failed to fetch bills: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BillsResponse> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    private void logBillData() {
        for (BillsResponse.Bill bill : bills) {
            Log.d(TAG, "Bill Start Date: " + bill.getBase().getBillStartDate());
            Log.d(TAG, "Bill End Date: " + bill.getBase().getBillEndDate());
            Log.d(TAG, "Total Cost: " + bill.getBase().getBillTotalCost());
            Log.d(TAG, "Total kWh: " + bill.getBase().getBillTotalKwh());
        }
    }

    public static List<BillsResponse.Bill> getBills() {
        return bills;
    }

    public double getTotalCost() {
        double totalCost = 0;
        for (BillsResponse.Bill bill : bills) {
            totalCost += bill.getBase().getBillTotalCost();
        }
        return totalCost;
    }

    public double getTotalUsage() {
        double totalUsage = 0;
        for (BillsResponse.Bill bill : bills) {
            totalUsage += bill.getBase().getBillTotalKwh();
        }
        return totalUsage;
    }

    public String getDateRange() {
        if (bills.isEmpty()) {
            return "No bills available";
        }
        String startDate = bills.get(0).getBase().getBillStartDate();
        String endDate = bills.get(bills.size() - 1).getBase().getBillEndDate();
        return "Date Range: " + startDate + " to " + endDate;
    }
}
