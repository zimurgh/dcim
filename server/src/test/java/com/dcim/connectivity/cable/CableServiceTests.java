package com.dcim.connectivity.cable;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.rackdeviceport.RackDevicePortService;
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
class CableServiceTests {

	@Autowired
	CableService cables;

	@Autowired
	RackDevicePortService ports;

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
	void addsCableThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto applied = applyAdd(
				AssetType.CABLE,
				cablePayload("CBL-1", seed.portAId(), seed.portBId(), seed.crossConnectId()));
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.CABLE);
		});

		CableDto current = cables.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.cableName()).isEqualTo("CBL-1");
		assertThat(current.portAId()).isEqualTo(seed.portAId());
		assertThat(current.portBId()).isEqualTo(seed.portBId());
		assertThat(current.crossConnectId()).isEqualTo(seed.crossConnectId());
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(cables.listCurrentByCrossConnect(seed.crossConnectId())).hasSize(1);
		assertThat(cables.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesCableThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		Long sparePortId = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"eth2\",\"rackDeviceId\":" + seed.deviceId()
						+ ",\"rackDevicePortTypeId\":" + seed.portTypeId() + "}")
				.assetIdentityId();

		ChangeDto added = applyAdd(
				AssetType.CABLE,
				cablePayload("CBL-1", seed.portAId(), seed.portBId(), seed.crossConnectId()));
		CableDto before = cables.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"cableName\":\"CBL-1B\",\"portAId\":" + seed.portAId()
						+ ",\"portBId\":" + sparePortId
						+ ",\"crossConnectId\":null}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CABLE,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.cableHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CableDto current = cables.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.cableName()).isEqualTo("CBL-1B");
		assertThat(current.portBId()).isEqualTo(sparePortId);
		assertThat(current.crossConnectId()).isNull();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(cables.listCurrentByCrossConnect(seed.crossConnectId())).isEmpty();
		assertThat(cables.history(added.assetIdentityId())).hasSize(2);
		assertThat(cables.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesCableThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto added = applyAdd(
				AssetType.CABLE,
				cablePayload("CBL-1", seed.portAId(), seed.portBId(), seed.crossConnectId()));
		CableDto before = cables.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CABLE,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.cableHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CableDto current = cables.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.cableName()).isEqualTo("CBL-1");
		assertThat(cables.history(added.assetIdentityId())).hasSize(2);
		assertThat(cables.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Seed seedDependencies() {
		Long ownerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"OwnerCo\"}").assetIdentityId();
		Long billingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"BillingCo\"}").assetIdentityId();
		Long latencyId = applyAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"Low Latency\",\"latencyType\":\"LL\"}")
				.assetIdentityId();
		Long speedId = applyAdd(
				AssetType.SPEED,
				"{\"speedName\":\"1 Gigabit\",\"speedType\":\"1G\"}")
				.assetIdentityId();
		Long crossConnectTypeId = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"Single Mode Fiber\"}")
				.assetIdentityId();
		Long crossConnectId = applyAdd(
				AssetType.CROSS_CONNECT,
				"{\"crossConnectName\":\"XC-1\",\"circuitId\":\"CKT-XC-1\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}")
				.assetIdentityId();

		Long dataCenterId = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}").assetIdentityId();
		Long cageId = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}")
				.assetIdentityId();
		Long rackId = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}")
				.assetIdentityId();
		Long deviceTypeId = applyAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"Extranet Switch\",\"rackDeviceTypeKind\":\"EXTRANET_SWITCH\"}")
				.assetIdentityId();
		Long deviceId = applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}")
				.assetIdentityId();
		Long portTypeId = applyAdd(
				AssetType.RACK_DEVICE_PORT_TYPE,
				"{\"rackDevicePortTypeName\":\"Copper\"}")
				.assetIdentityId();
		Long portAId = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}")
				.assetIdentityId();
		Long portBId = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"eth1\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}")
				.assetIdentityId();
		assertThat(ports.findCurrent(portAId)).isPresent();
		return new Seed(crossConnectId, deviceId, portTypeId, portAId, portBId);
	}

	private static String cablePayload(String name, Long portAId, Long portBId, Long crossConnectId) {
		return "{\"cableName\":\"" + name + "\",\"portAId\":" + portAId
				+ ",\"portBId\":" + portBId
				+ ",\"crossConnectId\":" + crossConnectId + "}";
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

	private record Seed(
			Long crossConnectId,
			Long deviceId,
			Long portTypeId,
			Long portAId,
			Long portBId) {
	}
}
