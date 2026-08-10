package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicLong;

import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.cable.CableService;
import com.dcim.connectivity.chargetype.ChargeTypeService;
import com.dcim.connectivity.crossconnect.CrossConnectService;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeService;
import com.dcim.connectivity.document.DocumentService;
import com.dcim.connectivity.latency.LatencyService;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedService;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeService;
import com.dcim.connectivity.speed.SpeedService;
import com.dcim.organization.exchange.ExchangeService;
import com.dcim.organization.firm.FirmService;
import com.dcim.organization.marketsegment.MarketSegmentService;
import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.organization.user.UserService;
import com.dcim.site.cage.CageService;
import com.dcim.site.datacenter.DataCenterService;
import com.dcim.site.rack.RackService;
import com.dcim.site.rackdevice.RackDeviceService;
import com.dcim.site.rackdeviceport.RackDevicePortService;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeService;
import com.dcim.site.rackdevicetype.RackDeviceTypeService;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeSpecDto;
import com.dcim.workflow.ChangeSpecService;
import com.dcim.workflow.ChangeSpecStatus;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.ChangeValidationResult;
import com.dcim.workflow.ValidationFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared fixtures and assertions for validation-rule coverage tests. Subclasses inherit the Spring
 * Boot test context (no need to repeat {@code @SpringBootTest} / {@code @Transactional}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class ValidationTestSupport {

	@Autowired
	ChangeService changes;

	@Autowired
	ChangeSpecService changeSpecs;

	@Autowired
	UserIdentityRepository userIdentities;

	@Autowired
	UserHistoryRepository userHistory;

	@Autowired
	FirmService firms;

	@Autowired
	ExchangeService exchanges;

	@Autowired
	MarketSegmentService marketSegments;

	@Autowired
	UserService users;

	@Autowired
	DataCenterService dataCenters;

	@Autowired
	CageService cages;

	@Autowired
	RackService racks;

	@Autowired
	RackDeviceTypeService rackDeviceTypes;

	@Autowired
	RackDeviceService rackDevices;

	@Autowired
	RackDevicePortTypeService rackDevicePortTypes;

	@Autowired
	RackDevicePortService rackDevicePorts;

	@Autowired
	CrossConnectService crossConnects;

	@Autowired
	CrossConnectTypeService crossConnectTypes;

	@Autowired
	ChargeTypeService chargeTypes;

	@Autowired
	LatencyService latencies;

	@Autowired
	SpeedService speeds;

	@Autowired
	MarketDataFeedService marketDataFeeds;

	@Autowired
	MarketDataFeedTypeService marketDataFeedTypes;

	@Autowired
	DocumentService documents;

	@Autowired
	CableService cables;

	Long appliedBy;

	private final AtomicLong sequence = new AtomicLong();

	@BeforeEach
	void seedTestUser() {
		appliedBy = TestUsers.seed(userIdentities, userHistory, "tester");
	}

	/** Generates a unique-per-test name so unrelated assertions never collide. */
	String unique(String prefix) {
		return prefix + "-" + sequence.incrementAndGet();
	}

	// ---------------------------------------------------------------- staging helpers

	ChangeDto stage(AssetType assetType, ChangeAction action, Long assetIdentityId, Long baseHistoryId, String payload) {
		ChangeDto untracked = changes.createUntracked(payload == null ? "{}" : payload, "tester");
		return changes.promoteToStaged(
				untracked.changeId(), assetType, action, assetIdentityId, baseHistoryId, null, "tester");
	}

	ChangeDto stageAdd(AssetType assetType, String payload) {
		return stage(assetType, ChangeAction.ADD, null, null, payload);
	}

	ChangeDto stageUpdate(AssetType assetType, Long assetIdentityId, Long baseHistoryId, String payload) {
		return stage(assetType, ChangeAction.UPDATE, assetIdentityId, baseHistoryId, payload);
	}

	ChangeDto stageTerminate(AssetType assetType, Long assetIdentityId, Long baseHistoryId) {
		return stage(assetType, ChangeAction.TERMINATE, assetIdentityId, baseHistoryId, "{}");
	}

	/** Stages a terminate against the identity's current history row. */
	ChangeDto stageTerminateCurrent(AssetType assetType, Long assetIdentityId) {
		return stageTerminate(assetType, assetIdentityId, currentHistoryId(assetType, assetIdentityId));
	}

	ChangeDto applyAdd(AssetType assetType, String payload) {
		ChangeDto staged = stageAdd(assetType, payload);
		return changes.applyStaged(staged.changeId(), appliedBy);
	}

	/** Looks up the current (VALID_TO is null) history id for an identity, regardless of asset type. */
	Long currentHistoryId(AssetType assetType, Long identityId) {
		return switch (assetType) {
			case DATA_CENTER -> dataCenters.findCurrent(identityId).orElseThrow().dataCenterHistoryId();
			case CAGE -> cages.findCurrent(identityId).orElseThrow().cageHistoryId();
			case RACK -> racks.findCurrent(identityId).orElseThrow().rackHistoryId();
			case RACK_DEVICE -> rackDevices.findCurrent(identityId).orElseThrow().rackDeviceHistoryId();
			case RACK_DEVICE_PORT -> rackDevicePorts.findCurrent(identityId).orElseThrow().rackDevicePortHistoryId();
			case RACK_DEVICE_TYPE -> rackDeviceTypes.findCurrent(identityId).orElseThrow().rackDeviceTypeHistoryId();
			case RACK_DEVICE_PORT_TYPE -> rackDevicePortTypes.findCurrent(identityId)
					.orElseThrow()
					.rackDevicePortTypeHistoryId();
			case CROSS_CONNECT -> crossConnects.findCurrent(identityId).orElseThrow().crossConnectHistoryId();
			case DOCUMENT -> documents.findCurrent(identityId).orElseThrow().documentHistoryId();
			case CABLE -> cables.findCurrent(identityId).orElseThrow().cableHistoryId();
			case LATENCY -> latencies.findCurrent(identityId).orElseThrow().latencyHistoryId();
			case SPEED -> speeds.findCurrent(identityId).orElseThrow().speedHistoryId();
			case CHARGE_TYPE -> chargeTypes.findCurrent(identityId).orElseThrow().chargeTypeHistoryId();
			case CROSS_CONNECT_TYPE -> crossConnectTypes.findCurrent(identityId)
					.orElseThrow()
					.crossConnectTypeHistoryId();
			case MARKET_DATA_FEED -> marketDataFeeds.findCurrent(identityId).orElseThrow().marketDataFeedHistoryId();
			case MARKET_DATA_FEED_TYPE -> marketDataFeedTypes.findCurrent(identityId)
					.orElseThrow()
					.marketDataFeedTypeHistoryId();
			case FIRM -> firms.findCurrent(identityId).orElseThrow().firmHistoryId();
			case EXCHANGE -> exchanges.findCurrent(identityId).orElseThrow().exchangeHistoryId();
			case MARKET_SEGMENT -> marketSegments.findCurrent(identityId).orElseThrow().marketSegmentHistoryId();
			case USER -> users.findCurrent(identityId).orElseThrow().userHistoryId();
		};
	}

	// ---------------------------------------------------------------- validation assertions

	ChangeValidationResult validate(Long changeId) {
		return changes.validateStaged(changeId);
	}

	void assertValid(Long changeId) {
		ChangeValidationResult result = changes.validateStaged(changeId);
		assertThat(result.issues())
				.withFailMessage("Expected no validation issues but found: %s", result.issues())
				.isEmpty();
	}

	void assertInvalid(Long changeId, String expectedCode) {
		ChangeValidationResult result = changes.validateStaged(changeId);
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).extracting(ValidationIssue::code).contains(expectedCode);
	}

	void assertApplySucceeds(Long changeId) {
		ChangeDto applied = changes.applyStaged(changeId, appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
	}

	void assertApplyBlocked(Long changeId, String expectedCode) {
		assertThatThrownBy(() -> changes.applyStaged(changeId, appliedBy))
				.isInstanceOf(ValidationFailedException.class)
				.satisfies(ex -> {
					ValidationFailedException failed = (ValidationFailedException) ex;
					assertThat(failed.getIssues()).extracting(ValidationIssue::code).contains(expectedCode);
				});
	}

	// ---------------------------------------------------------------- Change Spec helpers

	ChangeSpecDto createSpec(Long ownerFirmId) {
		return changeSpecs.create(ownerFirmId, unique("Spec"), "tester");
	}

	void addToSpec(Long specId, Long changeId) {
		changeSpecs.addChange(specId, changeId);
	}

	ChangeSpecDto submitPendingBillingWithChrec(Long specId) {
		changeSpecs.linkChrec(specId, unique("CHREC"), "Test CHREC", "https://jira.example/CHREC");
		return changeSpecs.submitPendingBilling(specId);
	}

	void assertSpecValid(Long specId) {
		ChangeValidationResult result = changeSpecs.validate(specId);
		assertThat(result.issues())
				.withFailMessage("Expected no validation issues but found: %s", result.issues())
				.isEmpty();
	}

	void assertSpecInvalid(Long specId, String expectedCode) {
		ChangeValidationResult result = changeSpecs.validate(specId);
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).extracting(ValidationIssue::code).contains(expectedCode);
	}

	void assertSpecApplySucceeds(Long specId) {
		ChangeSpecDto applied = changeSpecs.apply(specId, appliedBy);
		assertThat(applied.status()).isEqualTo(ChangeSpecStatus.APPLIED);
	}

	// ---------------------------------------------------------------- organization seeds

	Long seedFirm(String name) {
		return applyAdd(AssetType.FIRM, "{\"firmName\":\"" + name + "\"}").assetIdentityId();
	}

	Long seedExchange(String name, String type) {
		return applyAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"" + name + "\",\"exchangeCode\":\"" + name
						+ "\",\"exchangeAbbreviation\":\"" + name + "\",\"exchangeType\":\"" + type + "\"}")
				.assetIdentityId();
	}

	Long seedMarketSegment(String name, String type) {
		return applyAdd(
				AssetType.MARKET_SEGMENT,
				"{\"marketSegmentName\":\"" + name + "\",\"marketSegmentType\":\"" + type + "\"}")
				.assetIdentityId();
	}

	Long seedUser(String name) {
		return applyAdd(AssetType.USER, "{\"userName\":\"" + name + "\"}").assetIdentityId();
	}

	// ---------------------------------------------------------------- site seeds

	Long seedDataCenter(String name) {
		return applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + name + "\"}").assetIdentityId();
	}

	Long seedCage(String name, Long dataCenterId) {
		return applyAdd(AssetType.CAGE, "{\"cageName\":\"" + name + "\",\"dataCenterId\":" + dataCenterId + "}")
				.assetIdentityId();
	}

	Long seedRack(String name, Long cageId) {
		return applyAdd(AssetType.RACK, "{\"rackName\":\"" + name + "\",\"cageId\":" + cageId + "}")
				.assetIdentityId();
	}

	Long seedRackDeviceType(String name, String kind) {
		return applyAdd(
				AssetType.RACK_DEVICE_TYPE,
				"{\"rackDeviceTypeName\":\"" + name + "\",\"rackDeviceTypeKind\":\"" + kind + "\"}")
				.assetIdentityId();
	}

	Long seedRackDevicePortType(String name) {
		return applyAdd(AssetType.RACK_DEVICE_PORT_TYPE, "{\"rackDevicePortTypeName\":\"" + name + "\"}")
				.assetIdentityId();
	}

	Long seedRackDevice(String name, Long rackId, Long rackDeviceTypeId) {
		return applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"" + name + "\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + rackDeviceTypeId + "}")
				.assetIdentityId();
	}

	Long seedRackDevicePort(String name, Long rackDeviceId, Long rackDevicePortTypeId) {
		return applyAdd(
				AssetType.RACK_DEVICE_PORT,
				"{\"rackDevicePortName\":\"" + name + "\",\"rackDeviceId\":" + rackDeviceId
						+ ",\"rackDevicePortTypeId\":" + rackDevicePortTypeId + "}")
				.assetIdentityId();
	}

	/** Fresh Data Center -> Cage -> Rack chain, each with a unique name. */
	Long seedRackInNewTree() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		return seedRack(unique("Rack"), cageId);
	}

	record SiteDeviceFixture(Long rackId, Long rackDeviceTypeId, Long rackDeviceId) {
	}

	/** Fresh device (with its own type) attached to a fresh rack. */
	SiteDeviceFixture seedDeviceInNewTree() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		Long deviceId = seedRackDevice(unique("sw"), rackId, deviceTypeId);
		return new SiteDeviceFixture(rackId, deviceTypeId, deviceId);
	}

	record SitePortFixture(SiteDeviceFixture device, Long rackDevicePortTypeId, Long rackDevicePortId) {
	}

	/** Fresh port (with its own type) on a fresh device. */
	SitePortFixture seedPortInNewTree() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		return new SitePortFixture(device, portTypeId, portId);
	}

	/** Two distinct ports on the same fresh device, handy for cable tests. */
	Long[] seedPortPair() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portA = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		Long portB = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		return new Long[] { portA, portB };
	}

	// ---------------------------------------------------------------- connectivity seeds

	Long seedLatency(String name, String type) {
		return applyAdd(AssetType.LATENCY, "{\"latencyName\":\"" + name + "\",\"latencyType\":\"" + type + "\"}")
				.assetIdentityId();
	}

	Long seedSpeed(String name, String type) {
		return applyAdd(AssetType.SPEED, "{\"speedName\":\"" + name + "\",\"speedType\":\"" + type + "\"}")
				.assetIdentityId();
	}

	Long seedChargeType(String name) {
		return applyAdd(AssetType.CHARGE_TYPE, "{\"chargeTypeName\":\"" + name + "\"}").assetIdentityId();
	}

	Long seedCrossConnectType(String name) {
		return applyAdd(AssetType.CROSS_CONNECT_TYPE, "{\"crossConnectTypeName\":\"" + name + "\"}")
				.assetIdentityId();
	}

	Long seedMarketDataFeedType(String name) {
		return applyAdd(AssetType.MARKET_DATA_FEED_TYPE, "{\"marketDataFeedTypeName\":\"" + name + "\"}")
				.assetIdentityId();
	}

	/** Fresh owner/billing firms + cross connect type + latency (LL) + speed (1G). */
	record XcDeps(Long ownerFirmId, Long billingFirmId, Long crossConnectTypeId, Long latencyId, Long speedId) {
	}

	XcDeps seedXcDeps() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		Long billingFirmId = seedFirm(unique("Billing"));
		Long crossConnectTypeId = seedCrossConnectType(unique("XcType"));
		Long latencyId = seedLatency(unique("Latency"), "LL");
		Long speedId = seedSpeed(unique("Speed"), "1G");
		return new XcDeps(ownerFirmId, billingFirmId, crossConnectTypeId, latencyId, speedId);
	}

	String xcPayload(String circuitId, XcDeps deps) {
		return "{\"crossConnectName\":\"XC-" + circuitId + "\",\"circuitId\":\"" + circuitId + "\""
				+ ",\"crossConnectTypeId\":" + deps.crossConnectTypeId()
				+ ",\"latencyId\":" + deps.latencyId()
				+ ",\"speedId\":" + deps.speedId()
				+ ",\"ownerFirmId\":" + deps.ownerFirmId()
				+ ",\"billingFirmId\":" + deps.billingFirmId() + "}";
	}

	Long seedCrossConnect(String circuitId, XcDeps deps) {
		return applyAdd(AssetType.CROSS_CONNECT, xcPayload(circuitId, deps)).assetIdentityId();
	}

	Long seedMarketDataFeed(String name, Long crossConnectId, Long feedTypeId, XcDeps deps) {
		return applyAdd(
				AssetType.MARKET_DATA_FEED,
				"{\"marketDataFeedName\":\"" + name + "\",\"crossConnectId\":" + crossConnectId
						+ ",\"marketDataFeedTypeId\":" + feedTypeId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}")
				.assetIdentityId();
	}

	Long seedDocument(String name, Long crossConnectId) {
		return applyAdd(AssetType.DOCUMENT, "{\"documentName\":\"" + name + "\",\"crossConnectId\":" + crossConnectId + "}")
				.assetIdentityId();
	}

	Long seedCable(String name, Long portAId, Long portBId) {
		return applyAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + name + "\",\"portAId\":" + portAId + ",\"portBId\":" + portBId + "}")
				.assetIdentityId();
	}
}
