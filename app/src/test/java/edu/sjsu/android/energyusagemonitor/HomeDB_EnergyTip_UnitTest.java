package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class HomeDB_EnergyTip_UnitTest {

    public static class EnergyTipProvider {
        private static final String[] TIPS = {
                "Turn off lights when leaving a room.",
                "Unplug chargers when not in use.",
                "Use LED light bulbs.",
                "Set your thermostat a few degrees lower in winter.",
                "Wash clothes in cold water.",
                "Use natural light during the day.",
                "Seal windows and doors to prevent drafts.",
                "Use a programmable thermostat.",
                "Only run full loads in dishwasher and laundry.",
                "Air-dry clothes when possible."
        };

        public String getRandomEnergySavingTip() {
            Random random = new Random();
            return TIPS[random.nextInt(TIPS.length)];
        }
    }

    @Test
    public void testGetRandomEnergySavingTip_returnsOnlyValidTips() {
        EnergyTipProvider provider = new EnergyTipProvider();
        Set<String> validTips = new HashSet<>();
        validTips.add("Turn off lights when leaving a room.");
        validTips.add("Unplug chargers when not in use.");
        validTips.add("Use LED light bulbs.");
        validTips.add("Set your thermostat a few degrees lower in winter.");
        validTips.add("Wash clothes in cold water.");
        validTips.add("Use natural light during the day.");
        validTips.add("Seal windows and doors to prevent drafts.");
        validTips.add("Use a programmable thermostat.");
        validTips.add("Only run full loads in dishwasher and laundry.");
        validTips.add("Air-dry clothes when possible.");

        for (int i = 0; i < 100; i++) {
            String tip = provider.getRandomEnergySavingTip();
            assertTrue(validTips.contains(tip));
        }
    }
}
