package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.AuthorizationsResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.FormResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.HistoricalCollectionRequest;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.HistoricalCollectionResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.MeterResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.TestSubmitRequest;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.TestSubmitResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import edu.sjsu.android.energyusagemonitor.utilityapi.UtilityApiService;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private static final String API_TOKEN = "paste-api-key-from-discord";
    private UtilityApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://utilityapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(UtilityApiService.class);

        startQuickstart();
    }

    private void startQuickstart() {
        Call<FormResponse> createFormCall = apiService.createForm("Bearer " + API_TOKEN);
        createFormCall.enqueue(new Callback<FormResponse>() {
            @Override
            public void onResponse(Call<FormResponse> call, Response<FormResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String formUid = response.body().getUid();
                    testSubmitForm(formUid);
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to create form", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FormResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void testSubmitForm(String formUid) {
        TestSubmitRequest request = new TestSubmitRequest("DEMO", "residential");
        Call<TestSubmitResponse> testSubmitCall = apiService.testSubmitForm("Bearer " + API_TOKEN, formUid, request);
        testSubmitCall.enqueue(new Callback<TestSubmitResponse>() {
            @Override
            public void onResponse(Call<TestSubmitResponse> call, Response<TestSubmitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String referralCode = response.body().getReferral();
                    getAuthorizations(referralCode);
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to test submit form", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TestSubmitResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAuthorizations(String referralCode) {
        Call<AuthorizationsResponse> getAuthorizationsCall = apiService.getAuthorizations("Bearer " + API_TOKEN, referralCode, "meters");
        getAuthorizationsCall.enqueue(new Callback<AuthorizationsResponse>() {
            @Override
            public void onResponse(Call<AuthorizationsResponse> call, Response<AuthorizationsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String meterUid = response.body().getAuthorizations().get(0).getMeters().getMeters().get(0).getUid();

                    activateMeters(meterUid);
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to get authorizations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthorizationsResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void activateMeters(String meterUid) {
        HistoricalCollectionRequest request = new HistoricalCollectionRequest(List.of(meterUid), 12);
        Call<HistoricalCollectionResponse> activateMetersCall = apiService.activateMeters("Bearer " + API_TOKEN, request);
        activateMetersCall.enqueue(new Callback<HistoricalCollectionResponse>() {
            @Override
            public void onResponse(Call<HistoricalCollectionResponse> call, Response<HistoricalCollectionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    pollMeter(meterUid);
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to activate meters", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<HistoricalCollectionResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pollMeter(String meterUid) {
        Call<MeterResponse> getMeterCall = apiService.getMeter("Bearer " + API_TOKEN, meterUid);
        getMeterCall.enqueue(new Callback<MeterResponse>() {
            @Override
            public void onResponse(Call<MeterResponse> call, Response<MeterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getBillCount() > 0 && response.body().getStatus().equals("updated")) {
                        downloadBills(meterUid);
                    } else {
                        new android.os.Handler().postDelayed(() -> pollMeter(meterUid), 5000);
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to poll meter", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MeterResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadBills(String meterUid) {
        Call<BillsResponse> getBillsCall = apiService.getBills("Bearer " + API_TOKEN, meterUid);
        getBillsCall.enqueue(new Callback<BillsResponse>() {
            @Override
            public void onResponse(Call<BillsResponse> call, Response<BillsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SettingsActivity.this, "Bills downloaded!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to download bills", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BillsResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Already in Settings", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawers();
        return true;
    }
}