package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dcim.organization.firm.FirmDto;
import com.dcim.organization.firm.FirmService;
import com.dcim.site.cage.CageDto;
import com.dcim.site.cage.CageService;
import com.dcim.site.datacenter.DataCenterDto;
import com.dcim.site.datacenter.DataCenterService;
import com.dcim.site.rack.RackDto;
import com.dcim.site.rack.RackService;
import com.dcim.site.rackdevice.RackDeviceDto;
import com.dcim.site.rackdevice.RackDeviceService;
import com.dcim.site.rackdeviceport.RackDevicePortDto;
import com.dcim.site.rackdeviceport.RackDevicePortService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssetChangeLifecycleTests {

	@Autowired
	ChangeService changes;

	@Autowired
	ChangeCommittedHistoryRepository committedHistory;

	@Autowired
	FirmService firms;

	@Autowired
	DataCenterService dataCenters;

	@Autowired
	CageService cages;

	@Autowired
	RackService racks;

	@Autowired
	RackDeviceService devices;

	@Autowired
	RackDevicePortService ports;

	@Test
	void firmAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.FIRM,
				ChangeAction.ADD,
				null,
				null,
				"{\"firmName\":\"Acme\",\"parentFirmName\":\"HoldCo\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertThat(add.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.FIRM);
		});

		FirmDto current = firms.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.firmName()).isEqualTo("Acme");
		assertThat(current.parentFirmName()).isEqualTo("HoldCo");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(firms.history(add.assetIdentityId())).hasSize(1);
		assertCommittedLinks(add.changeId(), 1);

		ChangeDto update = progress(
				AssetType.FIRM,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.firmHistoryId(),
				"{\"firmName\":\"Acme Renamed\",\"parentFirmName\":\"HoldCo\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		FirmDto afterUpdate = firms.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.firmName()).isEqualTo("Acme Renamed");
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(firms.history(add.assetIdentityId())).hasSize(2);
		assertThat(firms.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.FIRM,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.firmHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		FirmDto afterTerminate = firms.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(firms.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void dataCenterAddThroughStagesWritesHistory() {
		ChangeDto applied = progress(
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				"{\"dataCenterName\":\"NY4\"}");
		DataCenterDto current = dataCenters.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("NY4");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(dataCenters.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.DATA_CENTER);
	}

	@Test
	void cageAddThroughStagesWritesHistory() {
		Long dataCenterId = progress(
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				"{\"dataCenterName\":\"NY4\"}").assetIdentityId();

		ChangeDto applied = progress(
				AssetType.CAGE,
				ChangeAction.ADD,
				null,
				null,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}");
		CageDto current = cages.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-A");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(cages.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.CAGE);
	}

	@Test
	void rackAddThroughStagesWritesHistory() {
		Long dataCenterId = progress(
				AssetType.DATA_CENTER, ChangeAction.ADD, null, null, "{\"dataCenterName\":\"NY4\"}")
				.assetIdentityId();
		Long cageId = progress(
				AssetType.CAGE,
				ChangeAction.ADD,
				null,
				null,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}")
				.assetIdentityId();

		ChangeDto applied = progress(
				AssetType.RACK,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}");
		RackDto current = racks.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R01");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(racks.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.RACK);
	}

	@Test
	void rackDeviceAddThroughStagesWritesHistory() {
		Long rackId = seedRack();
		ChangeDto applied = progress(
				AssetType.RACK_DEVICE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId + "}");
		RackDeviceDto current = devices.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw1");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(devices.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.RACK_DEVICE);
	}

	@Test
	void rackDevicePortAddThroughStagesWritesHistory() {
		Long rackId = seedRack();
		Long deviceId = progress(
				AssetType.RACK_DEVICE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId + "}")
				.assetIdentityId();

		ChangeDto applied = progress(
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId + "}");
		RackDevicePortDto current = ports.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(deviceId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(ports.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.RACK_DEVICE_PORT);
	}

	private Long seedRack() {
		Long dataCenterId = progress(
				AssetType.DATA_CENTER, ChangeAction.ADD, null, null, "{\"dataCenterName\":\"NY4\"}")
				.assetIdentityId();
		Long cageId = progress(
				AssetType.CAGE,
				ChangeAction.ADD,
				null,
				null,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}")
				.assetIdentityId();
		return progress(
				AssetType.RACK,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackName\":\"R01\",\"cageId\":" + cageId + "}")
				.assetIdentityId();
	}

	private ChangeDto progress(
			AssetType assetType,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			String payload) {
		ChangeDto untracked = changes.createUntracked(payload, "tester");
		assertThat(untracked.stage()).isEqualTo(ChangeStage.UNTRACKED);
		assertThat(untracked.statusLabel()).isEqualTo("Draft");

		ChangeDto staged = changes.promoteToStaged(
				untracked.changeId(),
				assetType,
				action,
				assetIdentityId,
				baseHistoryId,
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.assetType()).isEqualTo(assetType);
		assertThat(staged.action()).isEqualTo(action);

		ChangeDto committed = changes.applyStaged(untracked.changeId(), "tester");
		assertThat(committed.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(committed.assetIdentityId()).isNotNull();
		return committed;
	}

	private void assertCreatedLink(ChangeDto applied, AssetType assetType) {
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(assetType);
			assertThat(link.historyId()).isNotNull();
		});
		assertCommittedLinks(applied.changeId(), 1);
	}

	private void assertCommittedLinks(Long changeId, int expected) {
		assertThat(committedHistory.findByCommitted_ChangeId(changeId)).hasSize(expected);
	}
}
