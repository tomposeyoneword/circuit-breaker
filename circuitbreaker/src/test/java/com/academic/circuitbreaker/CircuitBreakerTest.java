package com.academic.circuitbreaker;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.academic.circuitbreaker.UserDataService.UserData;

@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerTest {

	@Mock
	private UserDataService mockUserDataService;

	private CircuitBreaker cb;

	@Before
	public void setUp() {
	}

	@Test
	public void testServiceSuccess() {
		int failureThreshold = 5;
		int timerDurationSeconds = 5;
		cb = new CircuitBreaker(new UserDataService(), failureThreshold, timerDurationSeconds);

		String name = "tom";
		UserData userData = cb.getUserData(name);
		assertNotNull(userData);
		assertEquals(userData.getName(), name);
		assertTrue(userData.getAge() > 18 && userData.getAge() < 55);
		assertEquals(cb.getState(), CircuitBreaker.GREEN);
	}

	@Test
	public void testFailureThreshold() {
		int failureThreshold = 5;
		int timerDurationSeconds = 5;
		cb = new CircuitBreaker(mockUserDataService, failureThreshold, timerDurationSeconds);

		// For each of the 5 calls (because 5 is our threshold) return null
		when(mockUserDataService.getUserData(any(String.class)))//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null);

		for (int i = 0; i < 5; i++) {
			UserData userData = cb.getUserData("blah " + i);
			assertNull(userData);
		}

		assertEquals(cb.getFailures(), 5);
		assertEquals(cb.getState(), CircuitBreaker.RED);
	}

	@Test
	public void testFailureThresholdTimerNotExpired() {
		int failureThreshold = 5;
		int timerDurationSeconds = 10;
		cb = new CircuitBreaker(mockUserDataService, failureThreshold, timerDurationSeconds);

		// The circuit breaker should never return this user data
		UserData invalidUserData = new UserData();
		invalidUserData.setName("SHOULD NOT GET RETURNED");

		// For each of the 5 calls (because 5 is our threshold) return null
		when(mockUserDataService.getUserData(any(String.class)))//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(invalidUserData);

		// This number of calls will go over the failureThreshold of 5. In this case the CircuitBreaker
		// should immediately return and not execute the UserDataService (once the threshold has been exceeded)
		for (int i = 0; i < 10; i++) {
			UserData userData = cb.getUserData("blah " + i);

			// All returns should be null
			assertNull(userData);
		}

		verify(mockUserDataService, times(5)).getUserData(any(String.class));
		assertEquals(cb.getFailures(), 5);
		assertEquals(cb.getState(), CircuitBreaker.RED);
	}

	@Test
	public void testFailureThresholdTimerExpiredServiceSuccess() throws InterruptedException {
		int failureThreshold = 5;
		int timerDurationSeconds = 5;
		cb = new CircuitBreaker(mockUserDataService, failureThreshold, timerDurationSeconds);

		// The circuit breaker should return this user data AFTER the timer expires
		UserData expectedUserData = new UserData();
		String name = "Tom-Expected";
		expectedUserData.setName(name);

		// For each of the 5 calls (because 5 is our threshold) return null
		when(mockUserDataService.getUserData(any(String.class)))//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(expectedUserData);

		// Hit the failure threshold
		for (int i = 0; i < 5; i++) {
			UserData userData = cb.getUserData("blah " + i);

			// All returns should be null
			assertNull(userData);
		}

		// Verify failure/red state
		verify(mockUserDataService, times(5)).getUserData(any(String.class));
		assertEquals(cb.getFailures(), 5);
		assertEquals(cb.getState(), CircuitBreaker.RED);

		// Sleep 5 seconds to allow the timer to expire
		Thread.sleep(5000);

		// We are using mocks so the "blah" parameter means nothing
		UserData actualUserData = cb.getUserData("blah");
		assertNotNull(actualUserData);
		assertEquals(actualUserData.getName(), expectedUserData.getName());
		assertEquals(cb.getFailures(), 0);
		assertEquals(cb.getState(), CircuitBreaker.GREEN);
	}

	@Test
	public void testFailureThresholdTimerExpiredServiceFailure() throws InterruptedException {
		int failureThreshold = 5;
		int timerDurationSeconds = 5;
		cb = new CircuitBreaker(mockUserDataService, failureThreshold, timerDurationSeconds);

		// The circuit breaker should never return this user data
		UserData expectedUserData = null;

		// For each of the 5 calls (because 5 is our threshold) return null
		when(mockUserDataService.getUserData(any(String.class)))//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(null)//
				.thenReturn(expectedUserData);

		// Hit the failure threshold
		for (int i = 0; i < 5; i++) {
			UserData userData = cb.getUserData("blah " + i);

			// All returns should be null
			assertNull(userData);
		}

		// Verify failure/red state
		assertEquals(cb.getFailures(), 5);
		assertEquals(cb.getState(), CircuitBreaker.RED);

		// Sleep 5 seconds to allow the timer to expire
		Thread.sleep(5000);

		// We are using mocks so the "blah" parameter means nothing
		UserData actualUserData = cb.getUserData("blah");
		verify(mockUserDataService, times(6)).getUserData(any(String.class));
		assertNull(actualUserData);
		assertEquals(cb.getFailures(), 6);
		assertEquals(cb.getState(), CircuitBreaker.RED);
	}
}
