package com.dcim.organization.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
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
class ExchangeServiceTests {

	@Autowired
	ExchangeService exchanges;

	@Autowired
	ChangeService changes;

	@Autowired
	UserIdentityRepository userIdentities;

	@Autowired
	UserHistoryRepository userHistory;

	Long appliedBy;

	@BeforeEach
	void seedUser() {
		appliedBy = TestUsers.seed(userIdentities, userHistory, "tester");
	}

	@Test
	void addsExchangeThroughChangeWorkflow() {
		ChangeDto applied = applyAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"Chicago Board Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"CBOE\",\"exchangeType\":\"OPTIONS\"}");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.EXCHANGE);
		});

		ExchangeDto current = exchanges.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.exchangeName()).isEqualTo("Chicago Board Options Exchange");
		assertThat(current.exchangeCode()).isEqualTo("CBOE");
		assertThat(current.exchangeAbbreviation()).isEqualTo("CBOE");
		assertThat(current.exchangeType()).isEqualTo(ExchangeType.OPTIONS);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(exchanges.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesExchangeThroughChangeWorkflow() {
		ChangeDto added = applyAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"Chicago Board Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"CBOE\",\"exchangeType\":\"OPTIONS\"}");
		ExchangeDto before = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"exchangeName\":\"Cboe Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"Cboe\",\"exchangeType\":\"OPTIONS\"}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.EXCHANGE,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.exchangeHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		ExchangeDto current = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.exchangeName()).isEqualTo("Cboe Options Exchange");
		assertThat(current.exchangeAbbreviation()).isEqualTo("Cboe");
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(exchanges.history(added.assetIdentityId())).hasSize(2);
		assertThat(exchanges.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesExchangeThroughChangeWorkflow() {
		ChangeDto added = applyAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"Chicago Board Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"CBOE\",\"exchangeType\":\"OPTIONS\"}");
		ExchangeDto before = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.EXCHANGE,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.exchangeHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		ExchangeDto current = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.exchangeCode()).isEqualTo("CBOE");
		assertThat(exchanges.history(added.assetIdentityId())).hasSize(2);
		assertThat(exchanges.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private ChangeDto applyAdd(AssetType assetType, String payload) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(
				draft.changeId(),
				assetType,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
