package com.academic.circuitbreaker;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.academic.circuitbreaker.UserDataService.UserData;

/**
 * https://medium.com/@soumendrak/circuit-breaker-design-pattern-997c3521c1c4
 */

@Component
public class CircuitBreaker {

	public static final int GREEN = 0;
	public static final int RED = 1;

	private static Instant timerThreshold;
	
	private final UserDataService service;
	private final int failureThreshold;
	private final int timerDurationSeconds;
	private final AtomicInteger state = new AtomicInteger(GREEN);
	private final AtomicInteger failures = new AtomicInteger(0);

	@Autowired
	public CircuitBreaker(UserDataService service, int failureThreshold, int timerDurationSeconds) {
		this.service = service;
		this.failureThreshold = failureThreshold;
		this.timerDurationSeconds = timerDurationSeconds;
	}

	public UserData getUserData(String name) {
		Instant now = Instant.now();

		if (state.get() == RED) {
			boolean isTimerExpired = now.isAfter(timerThreshold);
			if (!isTimerExpired) {
				// Timer has not expired. Remain in RED state & return false (null)
				return null;
			}

			// Otherwise, the Timer has expired. Continue on and try the service again.
		}

		UserData userData = service.getUserData(name);
		if (userData != null) {
			state.set(GREEN);
			failures.set(0);
			
			// Success. Return user data.
			return userData;
		}

		// Service failure. Increment the failure count & compare to the threshold.
		if (failures.incrementAndGet() >= failureThreshold) {

			// Failure threshold reached. Set the state to RED and start the timer
			state.set(RED);
			timerThreshold = Instant.now().plusSeconds(timerDurationSeconds);
		}
		
		// Service failure, return false (null)
		return null;
	}

	public int getState() {
		return state.get();
	}

	public int getFailures() {
		return failures.get();
	}
}
