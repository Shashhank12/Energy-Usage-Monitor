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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.R;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.gemini.UtilityBillAnalyzer;
import edu.sjsu.android.energyusagemonitor.ui.login.LoginActivity;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse.Bill;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

public class AnalysisActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AnalysisActivity";

    private UtilityBillAnalyzer billAnalyzer;
    private DrawerLayout drawerLayout;
    private PgeDataManager pgeDataManager;

    private List<Bill> apiBills = new ArrayList<>();
    private List<TimePeriodUsage> manualMonths = new ArrayList<>();

    private Spinner billSpinner;
    private Spinner analysisTypeSpinner;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private Markwon markwon;

    private int selectedItemPosition = 0;
    private String selectedAnalysisType = "general";
    private PgeDataManager.DataSource currentDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Energy Analysis");

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
        pgeDataManager = PgeDataManager.getInstance();

        loadDataBasedOnSource();
        setupBillSpinner();
        setupAnalysisTypeSpinner();

        findViewById(R.id.analyzeButton).setOnClickListener(v -> performAnalysis());
        findViewById(R.id.compareButton).setOnClickListener(v -> performComparison());
    }

    private void loadDataBasedOnSource() {
        currentDataSource = PgeDataManager.getActiveDataSource();
        Log.i(TAG, "Loading data for source: " + currentDataSource);

        apiBills.clear();
        manualMonths.clear();

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            manualMonths = pgeDataManager.getManualMonthlyUsage();
            if (manualMonths == null || manualMonths.isEmpty()) {
                showErrorAndFinish("No manual data available. Upload data in Settings.");
            } else {
                Collections.reverse(manualMonths);
            }
        } else {
            List<Bill> fetchedApiBills = SettingsActivity.getBills();
            if (fetchedApiBills == null || fetchedApiBills.isEmpty()) {
                showErrorAndFinish("No API/Demo data available. Connect in Settings.");
            } else {
                apiBills = new ArrayList<>(fetchedApiBills);
                Collections.reverse(apiBills);
            }
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setupBillSpinner() {
        List<String> displayStrings = new ArrayList<>();

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            for (TimePeriodUsage month : manualMonths) {
                if (month == null) continue;
                String start = formatDateSafe(month.getPeriodStart());
                String end = formatDateSafe(month.getPeriodEnd());
                String displayString = start + " to " + end + " - $" +
                        String.format(Locale.US, "%.2f", month.getTotalCost());
                displayStrings.add(displayString);
            }
        } else {
            for (Bill bill : apiBills) {
                if (bill == null || bill.getBase() == null) continue;
                String start = formatDateSafe(bill.getBase().getBillStartDate());
                String end = formatDateSafe(bill.getBase().getBillEndDate());
                String displayString = start + " to " + end + " - $" +
                        String.format(Locale.US, "%.2f", bill.getBase().getBillTotalCost());
                displayStrings.add(displayString);
            }
        }


        if (displayStrings.isEmpty()){
            Log.w(TAG, "No items to display in bill spinner.");
            displayStrings.add("No Data Available");
            findViewById(R.id.analyzeButton).setEnabled(false);
            findViewById(R.id.compareButton).setEnabled(false);
        } else {
            findViewById(R.id.analyzeButton).setEnabled(true);
            findViewById(R.id.compareButton).setEnabled(true);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                displayStrings
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        billSpinner.setAdapter(adapter);

        billSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int dataSize = (currentDataSource == PgeDataManager.DataSource.MANUAL) ? manualMonths.size() : apiBills.size();
                if (position >= 0 && position < dataSize) {
                    selectedItemPosition = position;
                } else {
                    selectedItemPosition = -1;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedItemPosition = -1;
            }
        });
    }

    private String formatDateSafe(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        try {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()));
        } catch (Exception e) {
            Log.w(TAG, "Failed to format LocalDateTime: " + dateTime);
            return "Invalid Date";
        }
    }

    private String formatDateSafe(String dateString) {
        if (dateString == null || dateString.equals("N/A")) return "N/A";
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()));
        } catch (DateTimeParseException e1) {
            try {
                LocalDate date = LocalDate.parse(dateString.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()));
            } catch (Exception e2) {
                Log.w(TAG, "Failed to parse Date String: " + dateString);
                return dateString.split("T")[0];
            }
        }
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
                selectedAnalysisType = "general";
            }
        });
    }

    private void performAnalysis() {
        if (selectedItemPosition < 0) {
            Toast.makeText(this, "No period selected or data unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("");

        UtilityBillAnalyzer.AnalysisCallback callback = new UtilityBillAnalyzer.AnalysisCallback() {
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
        };

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            if (selectedItemPosition >= manualMonths.size()) {
                Toast.makeText(this, "Selection out of bounds for manual data", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE); return;
            }
            TimePeriodUsage selectedMonth = manualMonths.get(selectedItemPosition);
            billAnalyzer.analyzeManualUsage(selectedMonth, selectedAnalysisType, callback);
        } else {
            if (selectedItemPosition >= apiBills.size()) {
                Toast.makeText(this, "Selection out of bounds for API data", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE); return;
            }
            Bill selectedBill = apiBills.get(selectedItemPosition);
            billAnalyzer.analyzeBill(selectedBill, selectedAnalysisType, callback);
        }
    }

    private void performComparison() {
        int dataSize = (currentDataSource == PgeDataManager.DataSource.MANUAL) ? manualMonths.size() : apiBills.size();

        if (dataSize < 2) {
            Toast.makeText(this, "Need at least 2 periods for comparison", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedItemPosition < 0 || selectedItemPosition >= dataSize) {
            Toast.makeText(this, "Invalid period selected", Toast.LENGTH_SHORT).show();
            return;
        }

        int previousItemIndex = selectedItemPosition + 1;
        if (previousItemIndex >= dataSize) {
            Toast.makeText(this, "Cannot compare the oldest period", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("");

        UtilityBillAnalyzer.AnalysisCallback callback = new UtilityBillAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(String analysisResult) { runOnUiThread(() -> { progressBar.setVisibility(View.GONE); markwon.setMarkdown(resultTextView, analysisResult); }); }
            @Override
            public void onError(String errorMessage) { runOnUiThread(() -> { progressBar.setVisibility(View.GONE); resultTextView.setText("Error: " + errorMessage); }); }
        };

        if (currentDataSource == PgeDataManager.DataSource.MANUAL) {
            TimePeriodUsage currentMonth = manualMonths.get(selectedItemPosition);
            TimePeriodUsage previousMonth = manualMonths.get(previousItemIndex);
            billAnalyzer.compareManualUsage(currentMonth, previousMonth, callback);
        } else {
            Bill currentBill = apiBills.get(selectedItemPosition);
            Bill previousBill = apiBills.get(previousItemIndex);
            billAnalyzer.compareBills(currentBill, previousBill, callback);
        }
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

                Intent intent = new Intent(AnalysisActivity.this, LoginActivity.class);
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