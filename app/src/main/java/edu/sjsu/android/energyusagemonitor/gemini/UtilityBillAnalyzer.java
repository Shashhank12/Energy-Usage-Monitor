package edu.sjsu.android.energyusagemonitor.gemini;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse.Bill;
import edu.sjsu.android.energyusagemonitor.utils.Constants;


public class UtilityBillAnalyzer {
    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final DateTimeFormatter internalIsoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
        if (bill == null || bill.getBase() == null) {
            callback.onError("Invalid API bill data provided.");
            return;
        }
        try {
            String startDateStr = bill.getBase().getBillStartDate();
            String endDateStr = bill.getBase().getBillEndDate();
            double totalCost = bill.getBase().getBillTotalCost();
            double totalKwh = bill.getBase().getBillTotalKwh();
            String serviceTariff = bill.getBase().getServiceTariff();

            String cacheKey = startDateStr + "-" + endDateStr + "-" + analysisType;

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildAnalysisPrompt(startDateStr, endDateStr, totalCost, totalKwh, serviceTariff, analysisType);
            generateContentInternal(prompt, cacheKey, callback);

        } catch (Exception e) {
            callback.onError("Error processing API bill data: " + e.getMessage());
        }
    }

    public void analyzeManualUsage(TimePeriodUsage usageData, String analysisType, AnalysisCallback callback) {
        if (usageData == null) {
            callback.onError("Invalid manual usage data provided.");
            return;
        }
        try {
            String startDateStr = formatLocalDateTimeToString(usageData.getPeriodStart());
            String endDateStr = formatLocalDateTimeToString(usageData.getPeriodEnd());
            double totalCost = usageData.getTotalCost();
            double totalKwh = usageData.getTotalKwh();
            String serviceTariff = "N/A";

            String cacheKey = startDateStr + "-" + endDateStr + "-" + analysisType;

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildAnalysisPrompt(startDateStr, endDateStr, totalCost, totalKwh, serviceTariff, analysisType);
            generateContentInternal(prompt, cacheKey, callback);

        } catch (Exception e) {
            callback.onError("Error processing manual usage data: " + e.getMessage());
        }
    }


    public void compareBills(Bill currentBill, Bill previousBill, AnalysisCallback callback) {
        if (currentBill == null || currentBill.getBase() == null || previousBill == null || previousBill.getBase() == null) {
            callback.onError("Invalid API bill data for comparison.");
            return;
        }
        try {
            String currentStart = currentBill.getBase().getBillStartDate();
            String currentEnd = currentBill.getBase().getBillEndDate();
            double currentCost = currentBill.getBase().getBillTotalCost();
            double currentKwh = currentBill.getBase().getBillTotalKwh();

            String prevStart = previousBill.getBase().getBillStartDate();
            String prevEnd = previousBill.getBase().getBillEndDate();
            double prevCost = previousBill.getBase().getBillTotalCost();
            double prevKwh = previousBill.getBase().getBillTotalKwh();

            String cacheKey = "compare-" + currentStart + "-" + prevStart;

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildComparisonPrompt(currentStart, currentEnd, currentCost, currentKwh, prevStart, prevEnd, prevCost, prevKwh);
            generateContentInternal(prompt, cacheKey, callback);

        } catch (Exception e) {
            callback.onError("Error processing API bills for comparison: " + e.getMessage());
        }
    }

    public void compareManualUsage(TimePeriodUsage currentPeriod, TimePeriodUsage previousPeriod, AnalysisCallback callback) {
        if (currentPeriod == null || previousPeriod == null) {
            callback.onError("Invalid manual usage data for comparison.");
            return;
        }
        try {
            String currentStart = formatLocalDateTimeToString(currentPeriod.getPeriodStart());
            String currentEnd = formatLocalDateTimeToString(currentPeriod.getPeriodEnd());
            double currentCost = currentPeriod.getTotalCost();
            double currentKwh = currentPeriod.getTotalKwh();

            String prevStart = formatLocalDateTimeToString(previousPeriod.getPeriodStart());
            String prevEnd = formatLocalDateTimeToString(previousPeriod.getPeriodEnd());
            double prevCost = previousPeriod.getTotalCost();
            double prevKwh = previousPeriod.getTotalKwh();

            String cacheKey = "compare-" + currentStart + "-" + prevStart;

            if (analysisCache.containsKey(cacheKey)) {
                callback.onSuccess(analysisCache.get(cacheKey));
                return;
            }

            String prompt = buildComparisonPrompt(currentStart, currentEnd, currentCost, currentKwh, prevStart, prevEnd, prevCost, prevKwh);
            generateContentInternal(prompt, cacheKey, callback);

        } catch (Exception e) {
            callback.onError("Error processing manual usage for comparison: " + e.getMessage());
        }
    }

    private void generateContentInternal(String prompt, String cacheKey, AnalysisCallback callback) {
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String resultText = result.getText();
                    if (resultText != null && !resultText.isEmpty()) {
                        analysisCache.put(cacheKey, resultText);
                        callback.onSuccess(resultText);
                    } else {
                        callback.onError("Analysis returned empty.");
                    }
                } catch (Exception e) {
                    callback.onError("Error processing analysis response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError("Analysis failed: " + t.getMessage());
            }
        }, executor);
    }


    private String buildAnalysisPrompt(String startDateStr, String endDateStr, double totalCost, double totalKwh, String serviceTariff, String analysisType) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this utility usage data with focus on ").append(analysisType).append(":\n\n");

        prompt.append("Period: ").append(formatApiOrManualDate(startDateStr))
                .append(" to ").append(formatApiOrManualDate(endDateStr)).append("\n");
        prompt.append("Total kWh: ").append(String.format(Locale.US, "%.2f", totalKwh)).append("\n");
        prompt.append("Total Cost: $").append(String.format(Locale.US, "%.2f", totalCost)).append("\n");
        if (serviceTariff != null && !serviceTariff.equals("N/A")) {
            prompt.append("Service Tariff: ").append(serviceTariff).append("\n");
        }
        prompt.append("\n");

        prompt.append("Provide analysis focusing on:\n");
        switch (analysisType) {
            case "cost_breakdown":
                prompt.append("1) Cost distribution (if possible)\n2) Most expensive components (if applicable)\n3) Potential savings opportunities\n");
                break;
            case "usage_patterns":
                prompt.append("1) Likely usage patterns\n2) Potential high consumption activities/times\n3) Comparison ideas for typical households\n");
                break;
            case "rate_analysis":
                prompt.append("1) Effectiveness of rate plan (if tariff known)\n2) Considerations for alternative plans\n3) Time-of-use thoughts\n");
                break;
            case "efficiency_tips":
                prompt.append("1) Specific energy efficiency recommendations\n2) Appliances potentially causing high usage\n3) Behavior changes suggestion\n");
                break;
            default:
                prompt.append("1) Key cost drivers insight\n2) General usage insights\n3) Personalized recommendations\n");
        }

        prompt.append("\nKeep response concise (max 200 words), structured with bullet points, ");
        prompt.append("and avoid technical jargon. Include specific numbers from the data.");

        return prompt.toString();
    }

    private String buildComparisonPrompt(String currentStart, String currentEnd, double currentCost, double currentKwh,
                                         String prevStart, String prevEnd, double prevCost, double prevKwh) throws Exception {

        double kwhChange = currentKwh - prevKwh;
        double percentChange = (prevKwh == 0) ? (kwhChange == 0 ? 0 : Double.POSITIVE_INFINITY * Math.signum(kwhChange)) : (kwhChange / prevKwh) * 100;

        double costChange = currentCost - prevCost;
        double costPercentChange = (prevCost == 0) ? (costChange == 0 ? 0 : Double.POSITIVE_INFINITY * Math.signum(costChange)) : (costChange / prevCost) * 100;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Compare these two utility usage periods and highlight key changes:\n\n");

        prompt.append("Current Period (").append(formatApiOrManualDate(currentStart))
                .append(" to ").append(formatApiOrManualDate(currentEnd)).append("):\n");
        prompt.append("- Total kWh: ").append(String.format(Locale.US, "%.2f", currentKwh)).append("\n");
        prompt.append("- Total Cost: $").append(String.format(Locale.US, "%.2f", currentCost)).append("\n\n");

        prompt.append("Previous Period (").append(formatApiOrManualDate(prevStart))
                .append(" to ").append(formatApiOrManualDate(prevEnd)).append("):\n");
        prompt.append("- Total kWh: ").append(String.format(Locale.US, "%.2f", prevKwh)).append("\n");
        prompt.append("- Total Cost: $").append(String.format(Locale.US, "%.2f", prevCost)).append("\n\n");

        prompt.append("Changes:\n");
        prompt.append("- kWh Change: ").append(String.format(Locale.US, "%.1f", kwhChange))
                .append(" (").append(Double.isInfinite(percentChange) ? "N/A" : String.format(Locale.US, "%.1f%%", percentChange)).append(")\n");
        prompt.append("- Cost Change: $").append(String.format(Locale.US, "%.2f", costChange))
                .append(" (").append(Double.isInfinite(costPercentChange) ? "N/A" : String.format(Locale.US, "%.1f%%", costPercentChange)).append(")\n\n");

        prompt.append("Analyze these changes and:\n");
        prompt.append("1) Identify significant usage pattern changes\n");
        prompt.append("2) Explain potential reasons for changes\n");
        prompt.append("3) Provide seasonally-adjusted insights if possible\n");
        prompt.append("4) Suggest actionable improvements\n\n");
        prompt.append("Keep response concise (max 250 words) and data-driven.");

        return prompt.toString();
    }

    private String formatApiOrManualDate(String dateString) {
        if (dateString == null || dateString.equalsIgnoreCase("N/A")) return "N/A";
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateString, internalIsoFormatter);
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
        } catch (DateTimeParseException e1) {
            try {
                SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
                Date date = standardFormat.parse(dateString);
                return displayFormat.format(date);
            } catch (Exception e2) {
                Log.w("DateParsing", "Could not parse date string for display: " + dateString);
                if (dateString.contains("T")) return dateString.split("T")[0];
                else return dateString;
            }
        }
    }

    private String formatLocalDateTimeToString(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        try {
            return dateTime.format(internalIsoFormatter);
        } catch (Exception e) {
            return "N/A";
        }
    }
}