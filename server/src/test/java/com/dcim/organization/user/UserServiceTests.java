package com.dcim.organization.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.HistoryLinkRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTests {

	@Autowired
	UserService users;

	@Autowired
	UserIdentityRepository identities;

	@Autowired
	UserHistoryRepository history;

	@Autowired
	ChangeService changes;

	Long appliedBy;

	@BeforeEach
	void seedApplier() {
		appliedBy = TestUsers.seed(identities, history, "tester", true);
	}

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
		ChangeDto draft = changes.createUntracked(
				"{\"userName\":\"bob\",\"isInitiator\":true}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.USER,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Add");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.USER);
		});

		UserDto current = users.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.userName()).isEqualTo("bob");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(users.history(applied.assetIdentityId())).hasSize(1);
		assertThat(users.listCurrent()).extracting(UserDto::userName).contains("bob");
	}

	@Test
	void updatesUserThroughChangeWorkflow() {
		ChangeDto added = applyAdd("{\"userName\":\"bob\",\"isInitiator\":false}");
		UserDto before = users.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"userName\":\"robert\",\"isInitiator\":true}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.USER,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.userHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		UserDto current = users.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.userName()).isEqualTo("robert");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(users.history(added.assetIdentityId())).hasSize(2);
		assertThat(users.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.userName()).isEqualTo("bob");
			assertThat(prior.isInitiator()).isFalse();
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesUserThroughChangeWorkflow() {
		ChangeDto added = applyAdd("{\"userName\":\"bob\",\"isInitiator\":true}");
		UserDto before = users.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.USER,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.userHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		UserDto current = users.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.userName()).isEqualTo("bob");
		assertThat(current.isInitiator()).isTrue();
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(users.history(added.assetIdentityId())).hasSize(2);
		assertThat(users.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private ChangeDto applyAdd(String payload) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(
				draft.changeId(),
				AssetType.USER,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
