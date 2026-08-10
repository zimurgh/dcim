package com.dcim.organization.user;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Seeds a current user for tests that need an APPLIED_BY user id.
 */
public final class TestUsers {

	private TestUsers() {
	}

	public static Long seed(UserIdentityRepository identities, UserHistoryRepository history, String userName) {
		return seed(identities, history, userName, false);
	}

	public static Long seed(
			UserIdentityRepository identities,
			UserHistoryRepository history,
			String userName,
			boolean isInitiator) {
		UserIdentity identity = identities.save(new UserIdentity());
		history.save(new UserHistory(
				identity,
				userName,
				isInitiator,
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				null,
				"ADD",
				"Active"));
		return identity.getUserId();
	}
}
