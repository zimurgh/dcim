package com.dcim.site.rackdeviceport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.rackdevice.RackDeviceIdentity;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentity;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentityRepository;
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
class RackDevicePortServiceTests {

	@Autowired
	RackDevicePortService ports;

	@Autowired
	RackDevicePortIdentityRepository portIdentities;

	@Autowired
	RackDevicePortHistoryRepository history;

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Autowired
	RackDevicePortTypeIdentityRepository portTypeIdentities;

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
	void listsAndLoadsCurrentPortUnderRackDevice() {
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());
		RackDevicePortTypeIdentity portType = portTypeIdentities.save(new RackDevicePortTypeIdentity());
		RackDevicePortIdentity port = portIdentities.save(new RackDevicePortIdentity());

		history.save(new RackDevicePortHistory(
				port,
				device,
				portType,
				"eth0",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"implement",
				null));

		assertThat(ports.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDevicePortId()).isEqualTo(port.getRackDevicePortId());
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackDevicePortTypeId()).isEqualTo(portType.getRackDevicePortTypeId());
			assertThat(row.rackDevicePortName()).isEqualTo("eth0");
		});

		assertThat(ports.listCurrentByRackDevice(device.getRackDeviceId())).hasSize(1);
		assertThat(ports.findCurrent(port.getRackDevicePortId())).get()
				.extracting(RackDevicePortDto::rackDevicePortName)
				.isEqualTo("eth0");
		assertThat(ports.history(port.getRackDevicePortId())).hasSize(1);
	}

	@Test
	void addsRackDevicePortThroughChangeWorkflow() {
		Long deviceId = seedRackDevice();
		Long portTypeId = seedPortType();

		ChangeDto draft = changes.createUntracked(
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE_PORT,
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
			assertThat(link.assetType()).isEqualTo(AssetType.RACK_DEVICE_PORT);
		});

		RackDevicePortDto current = ports.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(deviceId);
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(ports.history(applied.assetIdentityId())).hasSize(1);
		assertThat(ports.listCurrentByRackDevice(deviceId))
				.extracting(RackDevicePortDto::rackDevicePortName)
				.contains("eth0");
	}

	@Test
	void updatesRackDevicePortThroughChangeWorkflow() {
		Long deviceId = seedRackDevice();
		Long portTypeId = seedPortType();
		ChangeDto added = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}");
		RackDevicePortDto before = ports.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"rackDevicePortName\":\"eth1\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.rackDevicePortHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDevicePortDto current = ports.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth1");
		assertThat(current.rackDeviceId()).isEqualTo(deviceId);
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(ports.history(added.assetIdentityId())).hasSize(2);
		assertThat(ports.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.rackDevicePortName()).isEqualTo("eth0");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackDevicePortThroughChangeWorkflow() {
		Long deviceId = seedRackDevice();
		Long portTypeId = seedPortType();
		ChangeDto added = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}");
		RackDevicePortDto before = ports.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.rackDevicePortHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDevicePortDto current = ports.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(deviceId);
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(ports.history(added.assetIdentityId())).hasSize(2);
		assertThat(ports.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Long seedPortType() {
		return applyAdd(
				AssetType.RACK_DEVICE_PORT_TYPE,
				"{\"rackDevicePortTypeName\":\"Copper\"}")
				.assetIdentityId();
	}

	private Long seedRackDevice() {
		ChangeDto dataCenter = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");
		ChangeDto cage = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}");
		ChangeDto rack = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cage.assetIdentityId() + "}");
		Long deviceTypeId = applyAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"Extranet Switch\",\"rackDeviceTypeKind\":\"EXTRANET_SWITCH\"}")
				.assetIdentityId();
		return applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"sw-01\",\"rackId\":" + rack.assetIdentityId()
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}")
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
