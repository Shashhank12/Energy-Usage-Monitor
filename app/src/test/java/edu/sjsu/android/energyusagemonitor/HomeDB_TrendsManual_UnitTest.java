package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class HomeDB_TrendsManual_UnitTest {

    public static class TimePeriodUsage {
        private final double kwh;
        private final double cost;

        public TimePeriodUsage(double kwh, double cost) {
            this.kwh = kwh;
            this.cost = cost;
        }

        public double getTotalKwh() {
            return kwh;
        }

        public double getTotalCost() {
            return cost;
        }
    }

    public static class TrendAnalyzer {
        public double usagePercent = 0;
        public double costPercent = 0;

        public void updateTrendsFromManual(List<TimePeriodUsage> manualMonths) {
            if (manualMonths.size() < 2) return;

            TimePeriodUsage latest = manualMonths.get(manualMonths.size() - 1);
            TimePeriodUsage previous = manualMonths.get(manualMonths.size() - 2);

            usagePercent = calculatePercentageChange(latest.getTotalKwh(), previous.getTotalKwh());
            costPercent = calculatePercentageChange(latest.getTotalCost(), previous.getTotalCost());

            updateTrendTextViews(usagePercent, costPercent);
        }

        public void updateTrendTextViews(double usage, double cost) {
            // Override in test to capture values
        }

        public double calculatePercentageChange(double current, double previous) {
            if (previous == 0) {
                return (current == 0) ? 0 : (current > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
            }
            return ((current - previous) / previous) * 100.0;
        }
    }

    public static class TestableTrendAnalyzer extends TrendAnalyzer {
        public double calledUsage;
        public double calledCost;

        @Override
        public void updateTrendTextViews(double usage, double cost) {
            this.calledUsage = usage;
            this.calledCost = cost;
        }
    }

    @Test
    public void testUpdateTrendsFromManual_calculatesCorrectPercents() {
        TimePeriodUsage previous = new TimePeriodUsage(100.0, 50.0);
        TimePeriodUsage latest = new TimePeriodUsage(120.0, 75.0);

        TestableTrendAnalyzer analyzer = new TestableTrendAnalyzer();
        analyzer.updateTrendsFromManual(Arrays.asList(previous, latest));

        assertEquals(20.0, analyzer.calledUsage, 0.001);
        assertEquals(50.0, analyzer.calledCost, 0.001);
    }
}
