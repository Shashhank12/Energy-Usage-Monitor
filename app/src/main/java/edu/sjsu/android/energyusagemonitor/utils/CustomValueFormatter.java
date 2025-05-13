package edu.sjsu.android.energyusagemonitor.utils;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Locale;

public class CustomValueFormatter extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        return String.format(Locale.getDefault(), "%.0f kWh", value);
    }
}