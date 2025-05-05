package edu.sjsu.android.energyusagemonitor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Set;

import edu.sjsu.android.energyusagemonitor.activities.HomeDashboardActivity;
import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

public class HomeDashboardActivityTest {

    private PgeDataManager mockManager;

    @Before
    public void setUp() {
        mockManager = mock(PgeDataManager.class);
        PgeDataManager.setInstance(mockManager);
    }

    @Test
    public void testShowQueuedSnackbar() {
        HomeDashboardActivity activity = spy(new HomeDashboardActivity());
        activity.snackbarQueue = new LinkedList<>();
        activity.isSnackbarShowing = false;

        activity.showQueuedSnackbar("Test");

        assertEquals(1, activity.snackbarQueue.size());
        assertEquals("Test", activity.snackbarQueue.get(0));
        verify(activity).showNextSnackbar();
    }

    @Test
    public void testShowLoginNotifications() {
        HomeDashboardActivity activity = spy(new HomeDashboardActivity());
        activity.notifications = Arrays.asList("Msg1", "Msg2", "Msg3");

        doNothing().when(activity).showNotification(any(), anyString(), anyInt());

        activity.showLoginNotifications();

        verify(activity, times(3)).showNotification(eq(activity), anyString(), anyInt());
        verify(activity).showNotification(activity, "Msg1", 0);
        verify(activity).showNotification(activity, "Msg2", 1);
        verify(activity).showNotification(activity, "Msg3", 2);
    }

    @Test
    public void testUpdateTrendsFromManual() {
        HomeDashboardActivity activity = spy(new HomeDashboardActivity());

        TimePeriodUsage previous = mock(TimePeriodUsage.class);
        TimePeriodUsage latest = mock(TimePeriodUsage.class);

        when(previous.getTotalKwh()).thenReturn(100.0);
        when(latest.getTotalKwh()).thenReturn(120.0);
        when(previous.getTotalCost()).thenReturn(50.0);
        when(latest.getTotalCost()).thenReturn(75.0);

        doNothing().when(activity).updateTrendTextViews(anyDouble(), anyDouble());

        activity.updateTrendsFromManual(Arrays.asList(previous, latest));

        verify(activity).updateTrendTextViews(eq(20.0), eq(50.0));
    }

    @Test
    public void testUpdateTrendsFromApi() {
        HomeDashboardActivity activity = spy(new HomeDashboardActivity());

        BillsResponse.Base previousBase = mock(BillsResponse.Base.class);
        BillsResponse.Base latestBase = mock(BillsResponse.Base.class);

        when(previousBase.getBillTotalKwh()).thenReturn(200.0);
        when(latestBase.getBillTotalKwh()).thenReturn(250.0);
        when(previousBase.getBillTotalCost()).thenReturn(100.0);
        when(latestBase.getBillTotalCost()).thenReturn(150.0);

        BillsResponse.Bill previousBill = mock(BillsResponse.Bill.class);
        BillsResponse.Bill latestBill = mock(BillsResponse.Bill.class);

        when(previousBill.getBase()).thenReturn(previousBase);
        when(latestBill.getBase()).thenReturn(latestBase);

        doNothing().when(activity).updateTrendTextViews(anyDouble(), anyDouble());

        activity.updateTrendsFromApi(Arrays.asList(latestBill, previousBill));

        verify(activity).updateTrendTextViews(eq(25.0), eq(50.0));
    }

    @Test
    public void testCalculatePercentageChange() {
        HomeDashboardActivity activity = new HomeDashboardActivity();

        assertEquals(25.0, activity.calculatePercentageChange(125, 100), 0.001);
        assertEquals(-20.0, activity.calculatePercentageChange(80, 100), 0.001);
        assertEquals(0.0, activity.calculatePercentageChange(0, 0), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, activity.calculatePercentageChange(50, 0), 0.001);
        assertEquals(Double.NEGATIVE_INFINITY, activity.calculatePercentageChange(-50, 0), 0.001);
    }

    @Test
    public void testGetRandomEnergySavingTip_returnsOnlyValidTips() {
        HomeDashboardActivity activity = new HomeDashboardActivity();
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
            String tip = activity.getRandomEnergySavingTip();
            assertTrue(validTips.contains(tip));
        }
    }
}

