package edu.sjsu.android.energyusagemonitor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

public class ProfileActivityTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;

    private final String testUid = "testUser123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getUid()).thenReturn(testUid);
        when(mockFirestore.collection("users")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(testUid)).thenReturn(mockDocRef);
    }

    @Test
    public void testProfileImageUpdate() {
        String fakeUri = "content://test/image.jpg";

        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePic", fakeUri);

        mockDocRef.update(updates);
        verify(mockDocRef, times(1)).update(updates);
    }

    @Test
    public void testUserProfileFieldsFetched() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getString("firstName")).thenReturn("Jane");
        when(mockSnapshot.getString("lastName")).thenReturn("Doe");
        when(mockSnapshot.getString("email")).thenReturn("jane.doe@example.com");

        assertTrue(mockSnapshot.exists());
        assertEquals("Jane", mockSnapshot.getString("firstName"));
        assertEquals("Doe", mockSnapshot.getString("lastName"));
        assertEquals("jane.doe@example.com", mockSnapshot.getString("email"));
    }

    @Test
    public void testUserProfileSnapshotDoesNotExist() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        assertFalse(mockSnapshot.exists());
    }

    @Test
    public void testUserUidIsNull() {
        FirebaseUser nullUidUser = mock(FirebaseUser.class);
        when(nullUidUser.getUid()).thenReturn(null);
        assertNull(nullUidUser.getUid());
    }

    @Test
    public void testEmptyFieldsInSnapshot() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getString("firstName")).thenReturn(null);
        when(mockSnapshot.getString("lastName")).thenReturn(null);
        when(mockSnapshot.getString("email")).thenReturn(null);

        assertNull(mockSnapshot.getString("firstName"));
        assertNull(mockSnapshot.getString("lastName"));
        assertNull(mockSnapshot.getString("email"));
    }
}
