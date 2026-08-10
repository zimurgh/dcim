package com.dcim.site.rackdevice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rack.RackIdentityRepository;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentity;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentityRepository;
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
class RackDeviceServiceTests {

	@Autowired
	RackDeviceService rackDevices;

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Autowired
	RackDeviceHistoryRepository history;

	@Autowired
	RackIdentityRepository rackIdentities;

	@Autowired
	RackDeviceTypeIdentityRepository deviceTypeIdentities;

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
	void listsAndLoadsCurrentRackDeviceUnderRack() {
		RackIdentity rack = rackIdentities.save(new RackIdentity());
		RackDeviceTypeIdentity deviceType = deviceTypeIdentities.save(new RackDeviceTypeIdentity());
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());

		history.save(new RackDeviceHistory(
				device,
				rack,
				deviceType,
				"sw-01",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"implement",
				null));

		assertThat(rackDevices.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackId()).isEqualTo(rack.getRackId());
			assertThat(row.rackDeviceTypeId()).isEqualTo(deviceType.getRackDeviceTypeId());
			assertThat(row.rackDeviceName()).isEqualTo("sw-01");
		});

		assertThat(rackDevices.listCurrentByRack(rack.getRackId())).hasSize(1);
		assertThat(rackDevices.findCurrent(device.getRackDeviceId())).get()
				.extracting(RackDeviceDto::rackDeviceName)
				.isEqualTo("sw-01");
		assertThat(rackDevices.history(device.getRackDeviceId())).hasSize(1);
	}

	@Test
	void addsRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack();
		Long deviceTypeId = seedDeviceType();

		ChangeDto draft = changes.createUntracked(
				"{\"rackDeviceName\":\"sw-01\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE,
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
			assertThat(link.assetType()).isEqualTo(AssetType.RACK_DEVICE);
		});

		RackDeviceDto current = rackDevices.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-01");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(rackDevices.history(applied.assetIdentityId())).hasSize(1);
		assertThat(rackDevices.listCurrentByRack(rackId)).extracting(RackDeviceDto::rackDeviceName).contains("sw-01");
	}

	@Test
	void updatesRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack();
		Long deviceTypeId = seedDeviceType();
		ChangeDto added = applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"sw-01\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}");
		RackDeviceDto before = rackDevices.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"rackDeviceName\":\"sw-02\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.rackDeviceHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDeviceDto current = rackDevices.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-02");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevices.history(added.assetIdentityId())).hasSize(2);
		assertThat(rackDevices.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.rackDeviceName()).isEqualTo("sw-01");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack();
		Long deviceTypeId = seedDeviceType();
		ChangeDto added = applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"sw-01\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}");
		RackDeviceDto before = rackDevices.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.rackDeviceHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDeviceDto current = rackDevices.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-01");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevices.history(added.assetIdentityId())).hasSize(2);
		assertThat(rackDevices.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Long seedDeviceType() {
		return applyAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"Extranet Switch\",\"rackDeviceTypeKind\":\"EXTRANET_SWITCH\"}")
				.assetIdentityId();
	}

	private Long seedRack() {
		ChangeDto dataCenter = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");
		ChangeDto cage = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}");
		return applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cage.assetIdentityId() + "}")
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
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
