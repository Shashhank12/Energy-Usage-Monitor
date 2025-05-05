package edu.sjsu.android.energyusagemonitor;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeDB_LoginNotifications_UnitTest {

    public static class NotificationManager {
        public List<String> notifications = new ArrayList<>();

        public void showLoginNotifications() {
            for (int i = 0; i < notifications.size(); i++) {
                showNotification(notifications.get(i), i);
            }
        }

        public void showNotification(String message, int notificationId) {
            // override in test
        }
    }

    public static class TestableNotificationManager extends NotificationManager {
        public final List<String> calledMessages = new ArrayList<>();
        public final List<Integer> calledIds = new ArrayList<>();

        @Override
        public void showNotification(String message, int notificationId) {
            calledMessages.add(message);
            calledIds.add(notificationId);
        }
    }

    @Test
    public void testShowLoginNotifications_callsShowNotificationCorrectly() {
        TestableNotificationManager manager = new TestableNotificationManager();
        manager.notifications = Arrays.asList("Msg1", "Msg2", "Msg3");

        manager.showLoginNotifications();

        assertEquals(3, manager.calledMessages.size());
        assertEquals("Msg1", manager.calledMessages.get(0));
        assertEquals("Msg2", manager.calledMessages.get(1));
        assertEquals("Msg3", manager.calledMessages.get(2));

        assertEquals(Integer.valueOf(0), manager.calledIds.get(0));
        assertEquals(Integer.valueOf(1), manager.calledIds.get(1));
        assertEquals(Integer.valueOf(2), manager.calledIds.get(2));
    }
}
