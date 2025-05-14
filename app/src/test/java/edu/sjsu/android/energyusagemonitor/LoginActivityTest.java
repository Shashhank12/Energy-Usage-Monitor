package edu.sjsu.android.energyusagemonitor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


public class LoginActivityTest {

    @Mock
    FirebaseAuth mockAuth;
    private final String testEmail = "johndoe@test.com";
    private final String testPW = "TestPassword1!";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    public void testLoginSuccess() {
        Task<AuthResult> mockTask = Mockito.mock(Task.class);
        when(mockAuth.signInWithEmailAndPassword(testEmail, testPW)).thenReturn(mockTask);
        when(mockTask.isSuccessful()).thenReturn(true);

        Task<AuthResult> result = mockAuth.signInWithEmailAndPassword(testEmail, testPW);

        assertTrue(result.isSuccessful());
    }

    @Test
    public void testLoginFail() {
        Task<AuthResult> mockTask = Mockito.mock(Task.class);
        when(mockTask.isSuccessful()).thenReturn(false);
        when(mockAuth.signInWithEmailAndPassword(testEmail, "wrong")).thenReturn(mockTask);

        Task<AuthResult> result = mockAuth.signInWithEmailAndPassword(testEmail, "wrong");

        assertFalse(result.isSuccessful());
    }
}
