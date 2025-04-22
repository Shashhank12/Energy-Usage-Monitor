package edu.sjsu.android.energyusagemonitor.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PgeDataManager {

    private static final String TAG = "PgeDataManager";
    private static volatile PgeDataManager instance;

    public enum DataSource {
        API,
        MANUAL
    }

    private static volatile DataSource activeDataSource = DataSource.API;
    private final List<EnergyIntervalData> manualEnergyData = Collections.synchronizedList(new ArrayList<>());

    private static final String HEADER_DATE = "DATE";
    private static final String HEADER_START_TIME = "START TIME";
    private static final String HEADER_USAGE = "USAGE (kWh)";
    private static final String HEADER_COST = "COST";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm", Locale.US);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PgeDataManager() {}

    public static PgeDataManager getInstance() {
        if (instance == null) {
            synchronized (PgeDataManager.class) {
                if (instance == null) {
                    instance = new PgeDataManager();
                }
            }
        }
        return instance;
    }

    public static DataSource getActiveDataSource() {
        return activeDataSource;
    }

    public static void setActiveDataSource(DataSource source) {
        Log.i(TAG, "Setting active data source to: " + source);
        activeDataSource = source;
    }

    public void loadDataFromZip(Context context, Uri zipUri, Runnable onComplete, Runnable onError) {
        executor.execute(() -> {
            synchronized (manualEnergyData) {
                manualEnergyData.clear();
            }
            boolean success = parseZipStream(context.getContentResolver(), zipUri);
            if (success) {
                Log.i(TAG, "Successfully parsed and loaded manual data into memory.");
                if (onComplete != null) onComplete.run();
            } else {
                Log.e(TAG, "Failed to parse manual data from ZIP.");
                synchronized (manualEnergyData) {
                    manualEnergyData.clear();
                }
                if (onError != null) onError.run();
            }
        });
    }

    private boolean parseZipStream(ContentResolver resolver, Uri zipUri) {
        boolean csvFoundAndParsed = false;
        try (InputStream fileInputStream = resolver.openInputStream(zipUri);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
                    Log.d(TAG, "Parsing CSV entry: " + entry.getName());
                    parseCsvStream(zipInputStream);
                    csvFoundAndParsed = true;
                }
                zipInputStream.closeEntry();
            }
            if (!csvFoundAndParsed) { Log.w(TAG, "No CSV file found within ZIP."); return false; }
            synchronized (manualEnergyData) {
                manualEnergyData.sort((d1, d2) -> d1.getStartTimestamp().compareTo(d2.getStartTimestamp()));
            }
            Log.d(TAG, "Total manual interval records loaded: " + manualEnergyData.size());
            return true;
        } catch (IOException e) { Log.e(TAG, "Error reading ZIP stream", e); return false;
        } catch (Exception e) { Log.e(TAG, "Error parsing CSV data", e); return false; }
    }

    private void parseCsvStream(InputStream csvStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream));
        String line;
        int headerRowIndex = -1, lineNum = 0;
        int dateCol = -1, timeCol = -1, usageCol = -1, costCol = -1;

        while ((line = reader.readLine()) != null && lineNum <= 20) {
            lineNum++;
            if (line.contains(HEADER_DATE) && line.contains(HEADER_START_TIME) && line.contains(HEADER_USAGE)) {
                headerRowIndex = lineNum;
                String[] headers = line.split(",");
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim().replace("\"", "");
                    if (header.equalsIgnoreCase(HEADER_DATE)) dateCol = i;
                    else if (header.equalsIgnoreCase(HEADER_START_TIME)) timeCol = i;
                    else if (header.equalsIgnoreCase(HEADER_USAGE)) usageCol = i;
                    else if (header.equalsIgnoreCase(HEADER_COST)) costCol = i;
                }
                Log.d(TAG, String.format("Header found at line %d. Indices: Date=%d, Time=%d, Usage=%d, Cost=%d",
                        headerRowIndex, dateCol, timeCol, usageCol, costCol));
                break;
            }
        }

        if (headerRowIndex == -1 || dateCol == -1 || timeCol == -1 || usageCol == -1 || costCol == -1) {
            Log.e(TAG, "Could not find all required columns in header row."); return;
        }

        while ((line = reader.readLine()) != null) {
            lineNum++;
            String[] values = line.split(",");
            if (values.length <= Math.max(Math.max(dateCol, timeCol), Math.max(usageCol, costCol))) {
                Log.w(TAG, "Skipping line " + lineNum + ": Not enough columns ("+values.length+")"); continue;
            }
            try {
                String dateStr = values[dateCol].trim().replace("\"", "");
                String timeStr = values[timeCol].trim().replace("\"", "");
                String usageStr = values[usageCol].trim().replace("\"", "");
                String costStr = values[costCol].trim().replace("$", "").replace("\"", "");

                if (dateStr.contains("#")) { Log.w(TAG, "Skipping line " + lineNum + ": Invalid Date '#'."); continue; }

                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
                LocalDateTime timestamp = LocalDateTime.of(date, time);
                double usage = Double.parseDouble(usageStr);
                double cost = costStr.isEmpty() ? 0.0 : Double.parseDouble(costStr);

                synchronized (manualEnergyData) {
                    manualEnergyData.add(new EnergyIntervalData(timestamp, usage, cost));
                }
            } catch (DateTimeParseException e) { Log.w(TAG, "Skipping line " + lineNum + ": Date/Time parse error - " + e.getMessage());
            } catch (NumberFormatException e) { Log.w(TAG, "Skipping line " + lineNum + ": Number format error - " + e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) { Log.w(TAG, "Skipping line " + lineNum + ": Column index out of bounds."); }
        }
        Log.d(TAG, "Finished parsing stream. Current manual data size: " + manualEnergyData.size());
    }

    public List<EnergyIntervalData> getAllManualIntervalData() {
        synchronized (manualEnergyData) {
            return new ArrayList<>(manualEnergyData);
        }
    }

    public TimePeriodUsage getManualUsageForPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        double totalKwh = 0;
        double totalCost = 0;
        synchronized (manualEnergyData) {
            totalKwh = manualEnergyData.stream()
                    .filter(d -> d.getStartTimestamp() != null && !d.getStartTimestamp().isBefore(startDateTime) && d.getStartTimestamp().isBefore(endDateTime))
                    .mapToDouble(EnergyIntervalData::getUsageKWh)
                    .sum();
            totalCost = manualEnergyData.stream()
                    .filter(d -> d.getStartTimestamp() != null && !d.getStartTimestamp().isBefore(startDateTime) && d.getStartTimestamp().isBefore(endDateTime))
                    .mapToDouble(EnergyIntervalData::getCost)
                    .sum();
        }
        return new TimePeriodUsage(startDateTime, endDateTime.minusNanos(1), totalKwh, totalCost);
    }

    public List<TimePeriodUsage> getManualMonthlyUsage() {
        Map<YearMonth, MutableMonthData> monthlyTotals = new HashMap<>();
        synchronized (manualEnergyData) {
            for (EnergyIntervalData interval : manualEnergyData) {
                if (interval != null && interval.getStartTimestamp() != null) {
                    YearMonth ym = YearMonth.from(interval.getStartTimestamp());
                    MutableMonthData monthData = monthlyTotals.computeIfAbsent(ym, k -> new MutableMonthData(ym));
                    monthData.addKwh(interval.getUsageKWh());
                    monthData.addCost(interval.getCost());
                    monthData.updateStartEnd(interval.getStartTimestamp());
                }
            }
        }
        List<TimePeriodUsage> result = new ArrayList<>();
        for (MutableMonthData data : monthlyTotals.values()) {
            result.add(data.toTimePeriodUsage());
        }
        result.sort((m1, m2) -> m1.getPeriodStart().compareTo(m2.getPeriodStart()));
        return result;
    }

    private static class MutableMonthData {
        YearMonth month;
        double totalKwh = 0;
        double totalCost = 0;
        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        MutableMonthData(YearMonth ym) { this.month = ym; }
        void addKwh(double kwh) { this.totalKwh += kwh; }
        void addCost(double cost) { this.totalCost += cost; }
        void updateStartEnd(LocalDateTime timestamp) {
            if (earliest == null || timestamp.isBefore(earliest)) earliest = timestamp;
            if (latest == null || timestamp.isAfter(latest)) latest = timestamp;
        }
        TimePeriodUsage toTimePeriodUsage() {
            LocalDateTime start = (earliest != null) ? earliest : month.atDay(1).atStartOfDay();
            LocalDateTime end = (latest != null) ? latest : month.atEndOfMonth().atTime(23, 59, 59);
            return new TimePeriodUsage(start, end, totalKwh, totalCost);
        }
    }

    public void clearManualData() {
        synchronized (manualEnergyData) {
            manualEnergyData.clear();
            Log.d(TAG, "Cleared manual in-memory data.");
        }
    }
}