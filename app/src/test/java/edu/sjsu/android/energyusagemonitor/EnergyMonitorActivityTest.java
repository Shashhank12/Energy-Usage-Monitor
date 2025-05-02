package edu.sjsu.android.energyusagemonitor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.energyusagemonitor.upload.PgeDataManager;
import edu.sjsu.android.energyusagemonitor.upload.TimePeriodUsage;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;

public class EnergyMonitorActivityTest {

    private PgeDataManager mockManager;

    @Before
    public void setUp() {
        mockManager = mock(PgeDataManager.class);
        PgeDataManager.setInstance(mockManager);
    }

    // Test: Manual source with empty data should return 0 pages
    @Test
    public void testPaginateData_manualSource_emptyList() {
        List<TimePeriodUsage> manualData = new ArrayList<>();
        List<List<TimePeriodUsage>> paginated = paginateData(manualData, 5);
        assertTrue(paginated.isEmpty());
    }

    // Test: Manual source with 10 items should be split correctly into 3 pages (4+4+2)
    @Test
    public void testPaginateData_manualSource_validData() {
        List<TimePeriodUsage> manualData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            manualData.add(mock(TimePeriodUsage.class));
        }

        List<List<TimePeriodUsage>> pages = paginateData(manualData, 4);
        assertEquals(3, pages.size());
        assertEquals(4, pages.get(0).size());
        assertEquals(4, pages.get(1).size());
        assertEquals(2, pages.get(2).size());
    }

    // Test: API source with empty bill list should return 0 pages
    @Test
    public void testPaginateData_apiSource_emptyList() {
        List<BillsResponse.Bill> bills = new ArrayList<>();
        List<List<BillsResponse.Bill>> paginated = paginateData(bills, 3);
        assertTrue(paginated.isEmpty());
    }

    // Test helper for pagination logic
    private <T> List<List<T>> paginateData(List<T> data, int itemsPerPage) {
        List<List<T>> pages = new ArrayList<>();
        if (data == null || data.isEmpty() || itemsPerPage <= 0) {
            return pages;
        }
        for (int i = 0; i < data.size(); i += itemsPerPage) {
            int end = Math.min(i + itemsPerPage, data.size());
            pages.add(new ArrayList<>(data.subList(i, end)));
        }
        return pages;
    }

}
