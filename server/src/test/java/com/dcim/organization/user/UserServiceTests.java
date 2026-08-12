package com.dcim.organization.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTests extends ChangeTestSupport {

	@Autowired
	UserIdentityRepository identities;

	@Autowired
	UserHistoryRepository history;

	@Test
	void listsAndLoadsCurrentUser() {
		UserIdentity identity = identities.save(new UserIdentity());

		history.save(new UserHistory(
				identity,
				"alice",
				true,
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"ADD",
				"Active"));

		assertThat(users.listCurrent()).anySatisfy(user -> {
			assertThat(user.userId()).isEqualTo(identity.getUserId());
			assertThat(user.userName()).isEqualTo("alice");
			assertThat(user.isInitiator()).isTrue();
			assertThat(user.userHistoryId()).isNotNull();
		});

		assertThat(users.findCurrent(identity.getUserId())).get()
				.extracting(UserDto::userName)
				.isEqualTo("alice");
		assertThat(users.history(identity.getUserId())).hasSize(1);
	}

	@Test
	void addsUserThroughChangeWorkflow() {
		Long userId = applyAdd(
				"USER",
				json(Map.of("userName", "bob", "isInitiator", true)))
				.assetIdentityId();

		UserDto current = users.findCurrent(userId).orElseThrow();
		assertThat(current.userName()).isEqualTo("bob");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(users.history(userId)).hasSize(1);
		assertThat(users.listCurrent()).extracting(UserDto::userName).contains("bob");
	}

	@Test
	void updatesUserThroughChangeWorkflow() {
		Long userId = applyAdd(
				"USER",
				json(Map.of("userName", "bob", "isInitiator", false)))
				.assetIdentityId();

		applyUpdateCurrent(
				"USER",
				userId,
				json(Map.of("userName", "robert", "isInitiator", true)));

		UserDto current = users.findCurrent(userId).orElseThrow();
		assertThat(current.userName()).isEqualTo("robert");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(users.history(userId)).hasSize(2);
		assertThat(users.history(userId).getFirst()).satisfies(prior -> {
			assertThat(prior.userName()).isEqualTo("bob");
			assertThat(prior.isInitiator()).isFalse();
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesUserThroughChangeWorkflow() {
		Long userId = applyAdd(
				"USER",
				json(Map.of("userName", "bob", "isInitiator", true)))
				.assetIdentityId();

		applyTerminateCurrent("USER", userId);

		UserDto current = users.findCurrent(userId).orElseThrow();
		assertThat(current.userName()).isEqualTo("bob");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(users.history(userId)).hasSize(2);
		assertThat(users.history(userId).getFirst().validTo()).isNotNull();
	}
}
