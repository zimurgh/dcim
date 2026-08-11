package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;

import org.junit.jupiter.api.Test;

class SiteValidationTests extends ValidationTestSupport {

	@Test
	void dataCenterNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void dataCenterNameClashBlocksAddAndApply() {
		String name = unique("NY");
		seedDataCenter(name);

		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void cageNameAddSucceedsWhenUniqueWithinDataCenter() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		ChangeDto staged = stageAdd(
				AssetType.CAGE, "{\"cageName\":\"" + unique("Cage") + "\",\"dataCenterId\":" + dataCenterId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void cageNameClashWithinSameDataCenterBlocksAddAndApply() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		String cageName = unique("Cage");
		seedCage(cageName, dataCenterId);

		ChangeDto staged = stageAdd(
				AssetType.CAGE, "{\"cageName\":\"" + cageName + "\",\"dataCenterId\":" + dataCenterId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void cageNameAllowedAcrossDifferentDataCenters() {
		String cageName = unique("Cage");
		Long firstDataCenterId = seedDataCenter(unique("DC"));
		seedCage(cageName, firstDataCenterId);
		Long secondDataCenterId = seedDataCenter(unique("DC"));

		ChangeDto staged = stageAdd(
				AssetType.CAGE, "{\"cageName\":\"" + cageName + "\",\"dataCenterId\":" + secondDataCenterId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackNameAddSucceedsWhenUniqueWithinCage() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		ChangeDto staged = stageAdd(AssetType.RACK, "{\"rackName\":\"" + unique("Rack") + "\",\"cageId\":" + cageId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackNameClashWithinSameCageBlocksAddAndApply() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		String rackName = unique("Rack");
		seedRack(rackName, cageId);

		ChangeDto staged = stageAdd(AssetType.RACK, "{\"rackName\":\"" + rackName + "\",\"cageId\":" + cageId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void rackDeviceNameAddSucceedsWhenUniqueWithinRack() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"" + unique("sw") + "\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackDeviceNameClashWithinSameRackBlocksAddAndApply() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		String deviceName = unique("sw");
		seedRackDevice(deviceName, rackId, deviceTypeId);

		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"" + deviceName + "\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void rackDevicePortNameAddSucceedsWhenUniqueWithinDevice() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"" + unique("eth") + "\",\"rackDeviceId\":" + device.rackDeviceId()
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackDevicePortNameClashWithinSameDeviceBlocksAddAndApply() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		String portName = unique("eth");
		seedRackDevicePort(portName, device.rackDeviceId(), portTypeId);

		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"" + portName + "\",\"rackDeviceId\":" + device.rackDeviceId()
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void rackDeviceTypeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"" + unique("DeviceType") + "\",\"rackDeviceTypeKind\":\"TAP\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackDeviceTypeNameClashBlocksAddAndApply() {
		String name = unique("DeviceType");
		seedRackDeviceType(name, "TAP");

		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"" + name + "\",\"rackDeviceTypeKind\":\"PATCH_PANEL\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void rackDevicePortTypeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.RACK_DEVICE_PORT_TYPE, "{\"rackDevicePortTypeName\":\"" + unique("PortType") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void rackDevicePortTypeNameClashBlocksAddAndApply() {
		String name = unique("PortType");
		seedRackDevicePortType(name);

		ChangeDto staged = stageAdd(AssetType.RACK_DEVICE_PORT_TYPE, "{\"rackDevicePortTypeName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void terminateDataCenterBlockedByLiveCage() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		seedCage(unique("Cage"), dataCenterId);

		ChangeDto staged = stageTerminateCurrent(AssetType.DATA_CENTER, dataCenterId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateDataCenterSucceedsWhenNoLiveCages() {
		Long dataCenterId = seedDataCenter(unique("DC"));

		ChangeDto staged = stageTerminateCurrent(AssetType.DATA_CENTER, dataCenterId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateCageBlockedByLiveRack() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		seedRack(unique("Rack"), cageId);

		ChangeDto staged = stageTerminateCurrent(AssetType.CAGE, cageId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateCageSucceedsWhenNoLiveRacks() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);

		ChangeDto staged = stageTerminateCurrent(AssetType.CAGE, cageId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateRackBlockedByLiveDevice() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		seedRackDevice(unique("sw"), rackId, deviceTypeId);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK, rackId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateRackSucceedsWhenNoLiveDevices() {
		Long rackId = seedRackInNewTree();

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK, rackId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateRackDeviceBlockedByLivePort() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE, device.rackDeviceId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateRackDeviceSucceedsWhenNoLivePorts() {
		SiteDeviceFixture device = seedDeviceInNewTree();

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE, device.rackDeviceId());
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateRackDevicePortBlockedByLiveCable() {
		Long[] ports = seedPortPair();
		seedCable(unique("CBL"), ports[0], ports[1]);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_PORT, ports[0]);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateRackDevicePortSucceedsWhenNoLiveCable() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_PORT, portId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateRackDeviceTypeBlockedByActiveDevice() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		seedRackDevice(unique("sw"), rackId, deviceTypeId);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_TYPE, deviceTypeId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateRackDeviceTypeSucceedsWhenUnreferenced() {
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_TYPE, deviceTypeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateRackDevicePortTypeBlockedByActivePort() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_PORT_TYPE, portTypeId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateRackDevicePortTypeSucceedsWhenUnreferenced() {
		Long portTypeId = seedRackDevicePortType(unique("PortType"));

		ChangeDto staged = stageTerminateCurrent(AssetType.RACK_DEVICE_PORT_TYPE, portTypeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void batchTerminateDeviceAndItsPortsTogetherOnChangeSpecPasses() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		Long ownerFirmId = seedFirm(unique("Owner"));

		ChangeDto terminatePort = stageTerminateCurrent(AssetType.RACK_DEVICE_PORT, portId);
		ChangeDto terminateDevice = stageTerminateCurrent(AssetType.RACK_DEVICE, device.rackDeviceId());

		var spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), terminatePort.changeId());
		addToSpec(spec.changeSpecId(), terminateDevice.changeId());
		submitPendingBillingWithChrec(spec.changeSpecId());

		assertSpecValid(spec.changeSpecId());
		assertSpecApplySucceeds(spec.changeSpecId());

		assertThat(rackDevices.findCurrent(device.rackDeviceId()).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(rackDevicePorts.findCurrent(portId).orElseThrow().status()).isEqualTo("Terminated");
	}
}
