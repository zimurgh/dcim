package com.dcim.site.rack;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.site.cage.CageIdentity;
import com.dcim.site.cage.CageIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.HistoryLinkRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RackServiceTests {

	@Autowired
	RackService racks;

	@Autowired
	RackIdentityRepository rackIdentities;

	@Autowired
	RackHistoryRepository history;

	@Autowired
	CageIdentityRepository cageIdentities;

	@Autowired
	ChangeService changes;

	@Test
	void listsAndLoadsCurrentRackUnderCage() {
		CageIdentity cage = cageIdentities.save(new CageIdentity());
		RackIdentity rack = rackIdentities.save(new RackIdentity());

		history.save(new RackHistory(
				rack,
				cage,
				"R01",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(racks.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackId()).isEqualTo(rack.getRackId());
			assertThat(row.cageId()).isEqualTo(cage.getCageId());
			assertThat(row.rackName()).isEqualTo("R01");
		});

		assertThat(racks.listCurrentByCage(cage.getCageId())).hasSize(1);
		assertThat(racks.findCurrent(rack.getRackId())).get()
				.extracting(RackDto::rackName)
				.isEqualTo("R01");
		assertThat(racks.history(rack.getRackId())).hasSize(1);
	}

	@Test
	void addsRackThroughChangeWorkflow() {
		Long cageId = seedCage();

		ChangeDto draft = changes.createUntracked(
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Add");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.RACK);
		});

		RackDto current = racks.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R01");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(racks.history(applied.assetIdentityId())).hasSize(1);
		assertThat(racks.listCurrentByCage(cageId)).extracting(RackDto::rackName).contains("R01");
	}

	@Test
	void updatesRackThroughChangeWorkflow() {
		Long cageId = seedCage();
		ChangeDto added = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}");
		RackDto before = racks.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"rackName\":\"R02\",\"cageId\":" + cageId + "}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.rackHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDto current = racks.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R02");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(racks.history(added.assetIdentityId())).hasSize(2);
		assertThat(racks.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.rackName()).isEqualTo("R01");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackThroughChangeWorkflow() {
		Long cageId = seedCage();
		ChangeDto added = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}");
		RackDto before = racks.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.rackHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDto current = racks.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R01");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(racks.history(added.assetIdentityId())).hasSize(2);
		assertThat(racks.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Long seedCage() {
		ChangeDto dataCenter = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");
		return applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}")
				.assetIdentityId();
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
		return changes.applyStaged(draft.changeId(), "tester");
	}
}
