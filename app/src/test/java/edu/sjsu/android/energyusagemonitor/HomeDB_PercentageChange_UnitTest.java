package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeDB_PercentageChange_UnitTest {

    public static class PercentageCalculator {
        public static double calculatePercentageChange(double current, double previous) {
            if (previous == 0) {
                return (current == 0) ? 0 : (current > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
            }
            return ((current - previous) / previous) * 100.0;
        }
    }

    @Test
    public void testCalculatePercentageChange() {
        assertEquals(25.0, PercentageCalculator.calculatePercentageChange(125, 100), 0.001);
        assertEquals(-20.0, PercentageCalculator.calculatePercentageChange(80, 100), 0.001);
        assertEquals(0.0, PercentageCalculator.calculatePercentageChange(0, 0), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, PercentageCalculator.calculatePercentageChange(50, 0), 0.001);
        assertEquals(Double.NEGATIVE_INFINITY, PercentageCalculator.calculatePercentageChange(-50, 0), 0.001);
    }
}
