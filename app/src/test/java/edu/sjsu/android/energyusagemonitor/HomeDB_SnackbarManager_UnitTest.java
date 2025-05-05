package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.Queue;

public class HomeDB_SnackbarManager_UnitTest {

    public static class SnackbarManager {
        public Queue<String> snackbarQueue = new LinkedList<>();
        public boolean isSnackbarShowing = false;
        public boolean showNextCalled = false;

        public void showQueuedSnackbar(String message) {
            snackbarQueue.add(message);
            if (!isSnackbarShowing) {
                showNextSnackbar();
            }
        }

        public void showNextSnackbar() {
            showNextCalled = true;
        }
    }

    @Test
    public void testShowQueuedSnackbar_addsToQueueAndCallsShowNext() {
        SnackbarManager manager = new SnackbarManager();
        manager.showQueuedSnackbar("Test");

        assertEquals(1, manager.snackbarQueue.size());
        assertEquals("Test", manager.snackbarQueue.peek());
        assertTrue(manager.showNextCalled);
    }
}
