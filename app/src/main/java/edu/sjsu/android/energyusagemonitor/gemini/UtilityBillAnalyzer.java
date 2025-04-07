package edu.sjsu.android.energyusagemonitor.gemini;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse.Bill;
import edu.sjsu.android.energyusagemonitor.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UtilityBillAnalyzer {
    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private final Map<String, String> analysisCache = new HashMap<>();

    public UtilityBillAnalyzer() {
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", Constants.GEMINI_API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    public interface AnalysisCallback {
        void onSuccess(String analysisResult);
        void onError(String errorMessage);
    }

    public void analyzeBill(Bill bill, String analysisType, AnalysisCallback callback) {
        try {
            String cacheKey = bill.getBase().getBillStartDate() + "-" +
                    bill.getBase().getBillEndDate() + "-" +
                    analysisType;

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildAnalysisPrompt(bill, analysisType);

            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    analysisCache.put(cacheKey, resultText);
                    callback.onSuccess(resultText);
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onError("Analysis failed: " + t.getMessage());
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Error processing bill data: " + e.getMessage());
        }
    }

    public void compareBills(Bill currentBill, Bill previousBill, AnalysisCallback callback) {
        try {
            String cacheKey = "compare-" +
                    currentBill.getBase().getBillStartDate() + "-" +
                    previousBill.getBase().getBillStartDate();

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildComparisonPrompt(currentBill, previousBill);

            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    analysisCache.put(cacheKey, resultText);
                    callback.onSuccess(resultText);
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onError("Comparison failed: " + t.getMessage());
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Error processing bills: " + e.getMessage());
        }
    }

    private String buildAnalysisPrompt(Bill bill, String analysisType) throws Exception {
        Log.d("BillData", "Raw start date: " + bill.getBase().getBillStartDate());
        Log.d("BillData", "Raw end date: " + bill.getBase().getBillEndDate());
        BillsResponse.Base base = bill.getBase();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this utility bill with focus on ").append(analysisType).append(":\n\n");

        prompt.append("Billing Period: ").append(formatDate(base.getBillStartDate()))
                .append(" to ").append(formatDate(base.getBillEndDate())).append("\n");
        prompt.append("Total kWh: ").append(base.getBillTotalKwh()).append("\n");
        prompt.append("Total Cost: $").append(base.getBillTotalCost()).append("\n");
        prompt.append("Service Tariff: ").append(base.getServiceTariff()).append("\n\n");

        prompt.append("Provide analysis focusing on:\n");
        switch (analysisType) {
            case "cost_breakdown":
                prompt.append("1) Cost distribution\n");
                prompt.append("2) Most expensive components\n");
                prompt.append("3) Potential savings opportunities\n");
                break;

            case "usage_patterns":
                prompt.append("1) Usage patterns\n");
                prompt.append("2) High consumption periods\n");
                prompt.append("3) Comparison to typical household usage\n");
                break;

            case "rate_analysis":
                prompt.append("1) Effectiveness of current rate plan\n");
                prompt.append("2) Alternative rate plans to consider\n");
                prompt.append("3) Time-of-use opportunities\n");
                break;

            case "efficiency_tips":
                prompt.append("1) Specific energy efficiency recommendations\n");
                prompt.append("2) Appliances likely causing high usage\n");
                prompt.append("3) Behavior changes to reduce consumption\n");
                break;

            default:
                prompt.append("1) Key cost drivers\n");
                prompt.append("2) Usage insights\n");
                prompt.append("3) Personalized recommendations\n");
        }

        prompt.append("\nKeep response concise (max 200 words), structured with bullet points, ");
        prompt.append("and avoid technical jargon. Include specific numbers from the data.");

        return prompt.toString();
    }

    private String buildComparisonPrompt(Bill currentBill, Bill previousBill) throws Exception {
        BillsResponse.Base currentBase = currentBill.getBase();
        BillsResponse.Base previousBase = previousBill.getBase();

        double currentKwh = currentBase.getBillTotalKwh();
        double previousKwh = previousBase.getBillTotalKwh();
        double kwhChange = currentKwh - previousKwh;
        double percentChange = (kwhChange / previousKwh) * 100;

        double currentCost = currentBase.getBillTotalCost();
        double previousCost = previousBase.getBillTotalCost();
        double costChange = currentCost - previousCost;
        double costPercentChange = (costChange / previousCost) * 100;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Compare these two utility bills and highlight key changes:\n\n");

        prompt.append("Current Bill (").append(formatDate(currentBase.getBillStartDate()))
                .append(" to ").append(formatDate(currentBase.getBillEndDate())).append("):\n");
        prompt.append("- Total kWh: ").append(currentKwh).append("\n");
        prompt.append("- Total Cost: $").append(currentCost).append("\n\n");

        prompt.append("Previous Bill (").append(formatDate(previousBase.getBillStartDate()))
                .append(" to ").append(formatDate(previousBase.getBillEndDate())).append("):\n");
        prompt.append("- Total kWh: ").append(previousKwh).append("\n");
        prompt.append("- Total Cost: $").append(previousCost).append("\n\n");

        prompt.append("Changes:\n");
        prompt.append("- kWh Change: ").append(String.format(Locale.US, "%.1f", kwhChange))
                .append(" (").append(String.format(Locale.US, "%.1f%%", percentChange)).append(")\n");
        prompt.append("- Cost Change: $").append(String.format(Locale.US, "%.2f", costChange))
                .append(" (").append(String.format(Locale.US, "%.1f%%", costPercentChange)).append(")\n\n");

        prompt.append("Analyze these changes and:\n");
        prompt.append("1) Identify significant usage pattern changes\n");
        prompt.append("2) Explain potential reasons for changes\n");
        prompt.append("3) Provide seasonally-adjusted insights\n");
        prompt.append("4) Suggest actionable improvements\n\n");
        prompt.append("Keep response concise (max 250 words) and data-driven.");

        return prompt.toString();
    }

    private String formatDate(String apiDate) {
        try {
            try {
                SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                Date date = standardFormat.parse(apiDate);
                return displayFormat.format(date);
            } catch (Exception e) {
                if (apiDate.matches(".*\\.\\d{7}[-+]\\d{2}:\\d{2}")) {
                    String correctedDate = apiDate.replaceAll("(\\.\\d{6})\\d([-+]\\d{2}:\\d{2})", "$1$2");
                    SimpleDateFormat correctedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                    Date date = correctedFormat.parse(correctedDate);
                    return displayFormat.format(date);
                }
                else if (apiDate.matches("\\d{4}-\\d{4}T.*")) {
                    String correctedDate = apiDate.substring(0, 5) + "-" + apiDate.substring(5, 7) + apiDate.substring(7);
                    SimpleDateFormat correctedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                    Date date = correctedFormat.parse(correctedDate);
                    return displayFormat.format(date);
                }
                throw e;
            }
        } catch (Exception e) {
            Log.e("DateParsing", "Failed to parse date: " + apiDate, e);
            return apiDate.split("T")[0];
        }
    }
}