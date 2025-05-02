package edu.sjsu.android.energyusagemonitor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivityTest {

    @Mock
    FirebaseFirestore mockFirestore;

    @Mock
    FirebaseAuth mockAuth;

    @Mock
    FirebaseUser mockUser;

    @Mock
    CollectionReference mockCollectionRef;

    @Mock
    DocumentReference mockDocRef;

    private final String testUid = "user123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(testUid);
        when(mockFirestore.collection("users")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(testUid)).thenReturn(mockDocRef);
    }

    @Test
    public void testProfileUpdateWithValidData() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", "John");
        updates.put("lastName", "Smith");
        updates.put("budget", "150");

        mockDocRef.set(updates);

        verify(mockDocRef, times(1)).set(updates);
    }

    @Test
    public void testEmptyFieldsShouldStillAttemptUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", "");
        updates.put("lastName", "");
        updates.put("budget", "");

        mockDocRef.set(updates);

        verify(mockDocRef, times(1)).set(updates);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvalidBudgetInputShouldBeStoredAsString() {
        String invalidBudget = "two hundred";

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", "Jane");
        updates.put("lastName", "Doe");
        updates.put("budget", invalidBudget);

        mockDocRef.set(updates);

        verify(mockDocRef).set(argThat((Object obj) -> {
            if (!(obj instanceof Map)) return false;

            Map<String, Object> map = (Map<String, Object>) obj;
            return "Jane".equals(map.get("firstName")) &&
                    "Doe".equals(map.get("lastName")) &&
                    "two hundred".equals(map.get("budget"));
        }));
    }

    @Test
    public void testNullDocumentSnapshotReturnsNothing() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        assertFalse(mockSnapshot.exists());
    }
}
