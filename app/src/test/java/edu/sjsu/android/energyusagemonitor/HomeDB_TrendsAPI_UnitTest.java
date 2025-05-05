package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class HomeDB_TrendsAPI_UnitTest {

    public static class BillBase {
        private final double kwh;
        private final double cost;

        public BillBase(double kwh, double cost) {
            this.kwh = kwh;
            this.cost = cost;
        }

        public double getBillTotalKwh() {
            return kwh;
        }

        public double getBillTotalCost() {
            return cost;
        }
    }

    public static class Bill {
        private final BillBase base;

        public Bill(BillBase base) {
            this.base = base;
        }

        public BillBase getBase() {
            return base;
        }
    }

    public static class TrendAnalyzer {
        public double usagePercent = 0;
        public double costPercent = 0;

        public void updateTrendsFromApi(List<Bill> apiBills) {
            if (apiBills.size() < 2) return;

            Bill latest = apiBills.get(0);
            Bill previous = apiBills.get(1);

            if (latest.getBase() == null || previous.getBase() == null) return;

            usagePercent = calculatePercentageChange(
                    latest.getBase().getBillTotalKwh(),
                    previous.getBase().getBillTotalKwh()
            );
            costPercent = calculatePercentageChange(
                    latest.getBase().getBillTotalCost(),
                    previous.getBase().getBillTotalCost()
            );

            updateTrendTextViews(usagePercent, costPercent);
        }

        public void updateTrendTextViews(double usage, double cost) {
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
    public void testUpdateTrendsFromApi_calculatesCorrectPercents() {
        BillBase previousBase = new BillBase(200.0, 100.0);
        BillBase latestBase = new BillBase(250.0, 150.0);

        Bill previousBill = new Bill(previousBase);
        Bill latestBill = new Bill(latestBase);

        TestableTrendAnalyzer analyzer = new TestableTrendAnalyzer();
        analyzer.updateTrendsFromApi(Arrays.asList(latestBill, previousBill));

        assertEquals(25.0, analyzer.calledUsage, 0.001);
        assertEquals(50.0, analyzer.calledCost, 0.001);
    }
}
