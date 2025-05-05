package edu.sjsu.android.energyusagemonitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.sjsu.android.energyusagemonitor.activities.RegisterActivity;

public class RegisterActivityTest {
    @Mock
    FirebaseAuth mockAuth;

    @Mock
    DocumentReference mockDocRef;

    @Mock
    FirebaseUser mockUser;


    private final String testFirstName = "John";
    private final String testLastName = "Doe";
    private final String testEmail = "johndoe@test.com";
    private final String testPW = "TestPassword1!";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSendEmailVerification() {
        Task<Void> mockTask = Mockito.mock(Task.class);
        when(mockUser.sendEmailVerification()).thenReturn(mockTask);
        when(mockUser.isEmailVerified()).thenReturn(true);
        when(mockTask.isSuccessful()).thenReturn(true);

        Task<Void> result = mockUser.sendEmailVerification();

        assertTrue(result.isSuccessful());
        assertTrue(mockUser.isEmailVerified());
    }

    @Test
    public void testRegisterSuccess() {
        Task<AuthResult> mockTask = Mockito.mock(Task.class);
        when(mockAuth.createUserWithEmailAndPassword(testEmail, testPW)).thenReturn(mockTask);
        when(mockTask.isSuccessful()).thenReturn(true);

        Task<AuthResult> result = mockAuth.createUserWithEmailAndPassword(testEmail, testPW);

        assertTrue(result.isSuccessful());
    }

    @Test
    public void testRegisterFail() {
        Task<AuthResult> mockTask = Mockito.mock(Task.class);
        when(mockTask.isSuccessful()).thenReturn(false);
        when(mockAuth.createUserWithEmailAndPassword(testEmail, "wrong")).thenReturn(mockTask);

        Task<AuthResult> result = mockAuth.createUserWithEmailAndPassword(testEmail, "wrong");

        assertFalse(result.isSuccessful());
    }

    @Test
    public void testProfileEmptyInitialData() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", "");
        profile.put("lastName", "");
        profile.put("email", "");
        profile.put("budget", "");

        mockDocRef.set(profile);

        verify(mockDocRef, times(1)).set(profile);
    }

    @Test
    public void testProfileInitialData() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("firstName", testFirstName);
        profile.put("lastName", testLastName);
        profile.put("email", testEmail);
        profile.put("budget", "250");

        mockDocRef.set(profile);

        verify(mockDocRef, times(1)).set(profile);
    }

    @Test
    public void testPasswordMeetsReq() {
        String requirements = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^$*.{}()?!@#%;|]).{13,}$";
        assertTrue(testPW.matches(requirements));
    }

    @Test
    public void testPasswordDoesNotMeetReq() {
        String requirements = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^$*.{}()?!@#%;|]).{13,}$";
        assertFalse("wrong".matches(requirements));
    }
}
