package edu.sjsu.android.energyusagemonitor.uiBarchart;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.energyusagemonitor.R;

public class EnergyBarChartView extends BarChart {

    public EnergyBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChart(context);
    }

    private void initChart(Context context) {
        getLegend().setEnabled(false);
        getAxisLeft().setGranularity(1f);
        getAxisLeft().setAxisMinimum(0f);
        getAxisRight().setEnabled(false);

        setDrawGridBackground(false);
        setDrawBarShadow(false);
        setPinchZoom(false);
        setDrawValueAboveBar(true);

        setChartColors(context);
        setDescription(null);
    }

    public void setLabels(List<String> labels) {
        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
    }

    public void setData(List<Float> data) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i, data.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Energy Usage");
        applyDataSetColors(dataSet);
        BarData barData = new BarData(dataSet);

        super.setData(barData);
        invalidate();
    }

    private void setChartColors(Context context) {
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        int axisTextColor = isDarkMode ?
                ContextCompat.getColor(context, R.color.on_background_dark) :
                ContextCompat.getColor(context, R.color.on_background_light);

        int gridLineColor = isDarkMode ?
                ContextCompat.getColor(context, R.color.grid_dark) :
                ContextCompat.getColor(context, R.color.grid_light);

        getXAxis().setTextColor(axisTextColor);
        getXAxis().setGridColor(gridLineColor);

        getAxisLeft().setTextColor(axisTextColor);
        getAxisLeft().setGridColor(gridLineColor);
    }

    private void applyDataSetColors(BarDataSet dataSet) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        int barColor = isDarkMode ?
                ContextCompat.getColor(getContext(), R.color.primary_dark) :
                ContextCompat.getColor(getContext(), R.color.primary_light);

        dataSet.setColor(barColor);
        dataSet.setValueTextColor(barColor);
        dataSet.setValueTextSize(16f);
    }
}
