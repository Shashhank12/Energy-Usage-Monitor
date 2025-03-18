package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.energyusagemonitor.databinding.ActivitySettingsBinding;
import edu.sjsu.android.energyusagemonitor.utilityapi.RetrofitClient;
import edu.sjsu.android.energyusagemonitor.utilityapi.UtilityApiService;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private UtilityApiService apiService;
    private static List<BillsResponse.Bill> bills = new ArrayList<>();
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

    // Fetch bills and navigate to HomeDashboardActivity
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

                    Intent intent = new Intent(SettingsActivity.this, HomeDashboardActivity.class);
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
