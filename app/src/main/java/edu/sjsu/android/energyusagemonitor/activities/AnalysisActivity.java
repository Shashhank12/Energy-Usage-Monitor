package edu.sjsu.android.energyusagemonitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.gemini.UtilityBillAnalyzer;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse.Bill;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

public class AnalysisActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private UtilityBillAnalyzer billAnalyzer;
    private List<Bill> bills = new ArrayList<>();
    private DrawerLayout drawerLayout;

    private Spinner billSpinner;
    private Spinner analysisTypeSpinner;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private Markwon markwon;

    private int selectedBillPosition = 0;
    private String selectedAnalysisType = "general";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        markwon = Markwon.builder(this)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(LinkifyPlugin.create())
                .build();

        billSpinner = findViewById(R.id.billSpinner);
        analysisTypeSpinner = findViewById(R.id.analysisTypeSpinner);
        resultTextView = findViewById(R.id.resultTextView);
        progressBar = findViewById(R.id.progressBar);

        billAnalyzer = new UtilityBillAnalyzer();

        loadBillData();

        setupBillSpinner();
        setupAnalysisTypeSpinner();

        findViewById(R.id.analyzeButton).setOnClickListener(v -> performAnalysis());
        findViewById(R.id.compareButton).setOnClickListener(v -> performComparison());
    }

    private void loadBillData() {
        try {
            bills = SettingsActivity.getBills();

            if (bills == null || bills.isEmpty()) {
                Toast.makeText(this, "No bill data available", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading bill data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupBillSpinner() {
        List<String> billDisplayStrings = new ArrayList<>();
        for (Bill bill : bills) {
            String displayString = formatDateShort(bill.getBase().getBillStartDate()) + " to " +
                    formatDateShort(bill.getBase().getBillEndDate()) + " - $" +
                    bill.getBase().getBillTotalCost();
            billDisplayStrings.add(displayString);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                billDisplayStrings
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        billSpinner.setAdapter(adapter);

        billSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBillPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
        } else if (id == R.id.nav_energy_monitor) {
            startActivity(new Intent(this, EnergyMonitorActivity.class));
        } else if (id == R.id.nav_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(AnalysisActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupAnalysisTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.analysis_types,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        analysisTypeSpinner.setAdapter(adapter);

        analysisTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = getResources().getStringArray(R.array.analysis_type_values);
                selectedAnalysisType = values[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void performAnalysis() {
        if (bills.isEmpty()) {
            Toast.makeText(this, "No bill data available", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("");

        Bill selectedBill = bills.get(selectedBillPosition);

        billAnalyzer.analyzeBill(selectedBill, selectedAnalysisType, new UtilityBillAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(String analysisResult) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    markwon.setMarkdown(resultTextView, analysisResult);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resultTextView.setText("Error: " + errorMessage);
                });
            }
        });
    }

    private void performComparison() {
        if (bills.size() < 2) {
            Toast.makeText(this, "Need at least 2 bills for comparison", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("");

        Bill currentBill = bills.get(selectedBillPosition);
        Bill previousBill = bills.get(Math.min(selectedBillPosition + 1, bills.size() - 1));

        billAnalyzer.compareBills(currentBill, previousBill, new UtilityBillAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(String analysisResult) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    markwon.setMarkdown(resultTextView, analysisResult);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resultTextView.setText("Error: " + errorMessage);
                });
            }
        });
    }

    private String formatDateShort(String apiDate) {
        try {
            if (apiDate.matches("\\d{4}-\\d{4}T.*")) {
                String correctedDate = apiDate.substring(0, 5) + "-" + apiDate.substring(5, 7) + apiDate.substring(7);
                apiDate = correctedDate;
            }

            if (apiDate.matches(".*\\.\\d{7}[-+]\\d{2}:\\d{2}")) {
                apiDate = apiDate.replaceAll("(\\.\\d{6})\\d([-+]\\d{2}:\\d{2})", "$1$2");
            }

            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            Date date = apiFormat.parse(apiDate);
            return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date);
        } catch (Exception e) {
            Log.e("DateParsing", "Failed to parse date: " + apiDate, e);
            return apiDate.split("T")[0];
        }
    }
}