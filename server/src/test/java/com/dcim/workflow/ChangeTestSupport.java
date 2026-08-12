package com.dcim.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
import com.dcim.workflow.assettype.AssetTypeService;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ChangeTestSupport {

	@Autowired
	protected ChangeService changes;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected UserIdentityRepository userIdentities;

	@Autowired
	protected UserHistoryRepository userHistory;

	@Autowired
	protected FirmService firms;

	@Autowired
	protected ExchangeService exchanges;

	@Autowired
	protected MarketSegmentService marketSegments;

	@Autowired
	protected UserService users;

	@Autowired
	protected DataCenterService dataCenters;

	@Autowired
	protected CageService cages;

	@Autowired
	protected RackService racks;

	@Autowired
	protected RackDeviceTypeService rackDeviceTypes;

	@Autowired
	protected RackDeviceService rackDevices;

	@Autowired
	protected RackDevicePortTypeService rackDevicePortTypes;

	@Autowired
	protected RackDevicePortService rackDevicePorts;

	@Autowired
	protected CrossConnectService crossConnects;

	@Autowired
	protected CrossConnectTypeService crossConnectTypes;

	@Autowired
	protected ChargeTypeService chargeTypes;

	@Autowired
	protected LatencyService latencies;

	@Autowired
	protected SpeedService speeds;

	@Autowired
	protected MarketDataFeedService marketDataFeeds;

	@Autowired
	protected MarketDataFeedTypeService marketDataFeedTypes;

	@Autowired
	protected DocumentService documents;

	@Autowired
	protected CableService cables;

	@Autowired
	protected AssetTypeService assetTypes;

	protected Long appliedBy;

	private final AtomicLong sequence = new AtomicLong();

	@BeforeEach
	void seedTestUser() {
		appliedBy = TestUsers.seed(userIdentities, userHistory, "tester");
	}

	/** Unique-per-test name so unrelated assertions never collide. */
	protected String unique(String prefix) {
		return prefix + "-" + sequence.incrementAndGet();
	}

	/** Serializes a map to a change payload JSON object. */
	protected String json(Map<String, ?> fields) {
		return objectMapper.writeValueAsString(fields);
	}

	/** Convenience builder when Map.of's arity/null limits get in the way. */
	protected static Map<String, Object> fields(Object... keyValues) {
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("fields requires even number of arguments");
		}
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	protected ChangeDto stage(
			String assetType,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			String payload) {
		ChangeDto untracked = changes.createUntracked(payload == null ? "{}" : payload, "tester");
		return changes.promoteToStaged(
				untracked.changeId(), assetType, action, assetIdentityId, baseHistoryId, null, "tester");
	}

	protected ChangeDto stageAdd(String assetType, String payload) {
		return stage(assetType, ChangeAction.ADD, null, null, payload);
	}

	protected ChangeDto stageUpdate(String assetType, Long assetIdentityId, Long baseHistoryId, String payload) {
		return stage(assetType, ChangeAction.UPDATE, assetIdentityId, baseHistoryId, payload);
	}

	protected ChangeDto stageTerminate(String assetType, Long assetIdentityId, Long baseHistoryId) {
		return stage(assetType, ChangeAction.TERMINATE, assetIdentityId, baseHistoryId, "{}");
	}

	protected ChangeDto stageTerminateCurrent(String assetType, Long assetIdentityId) {
		return stageTerminate(assetType, assetIdentityId, currentHistoryId(assetType, assetIdentityId));
	}

	protected ChangeDto applyAdd(String assetType, String payload) {
		return changes.applyStaged(stageAdd(assetType, payload).changeId(), appliedBy);
	}

	protected ChangeDto applyUpdate(
			String assetType,
			Long assetIdentityId,
			Long baseHistoryId,
			String payload) {
		return changes.applyStaged(
				stageUpdate(assetType, assetIdentityId, baseHistoryId, payload).changeId(),
				appliedBy);
	}

	protected ChangeDto applyUpdateCurrent(String assetType, Long assetIdentityId, String payload) {
		return applyUpdate(assetType, assetIdentityId, currentHistoryId(assetType, assetIdentityId), payload);
	}

	protected ChangeDto applyTerminate(String assetType, Long assetIdentityId, Long baseHistoryId) {
		return changes.applyStaged(
				stageTerminate(assetType, assetIdentityId, baseHistoryId).changeId(),
				appliedBy);
	}

	protected ChangeDto applyTerminateCurrent(String assetType, Long assetIdentityId) {
		return applyTerminate(assetType, assetIdentityId, currentHistoryId(assetType, assetIdentityId));
	}

	/** Current (VALID_TO is null) history id for an identity. */
	protected Long currentHistoryId(String assetType, Long identityId) {
		return switch (assetType) {
			case "DATA_CENTER" -> dataCenters.findCurrent(identityId).orElseThrow().dataCenterHistoryId();
			case "CAGE" -> cages.findCurrent(identityId).orElseThrow().cageHistoryId();
			case "RACK" -> racks.findCurrent(identityId).orElseThrow().rackHistoryId();
			case "RACK_DEVICE" -> rackDevices.findCurrent(identityId).orElseThrow().rackDeviceHistoryId();
			case "RACK_DEVICE_PORT" -> rackDevicePorts.findCurrent(identityId).orElseThrow().rackDevicePortHistoryId();
			case "RACK_DEVICE_TYPE" -> rackDeviceTypes.findCurrent(identityId).orElseThrow().rackDeviceTypeHistoryId();
			case "RACK_DEVICE_PORT_TYPE" -> rackDevicePortTypes.findCurrent(identityId)
					.orElseThrow()
					.rackDevicePortTypeHistoryId();
			case "CROSS_CONNECT" -> crossConnects.findCurrent(identityId).orElseThrow().crossConnectHistoryId();
			case "DOCUMENT" -> documents.findCurrent(identityId).orElseThrow().documentHistoryId();
			case "CABLE" -> cables.findCurrent(identityId).orElseThrow().cableHistoryId();
			case "LATENCY" -> latencies.findCurrent(identityId).orElseThrow().latencyHistoryId();
			case "SPEED" -> speeds.findCurrent(identityId).orElseThrow().speedHistoryId();
			case "CHARGE_TYPE" -> chargeTypes.findCurrent(identityId).orElseThrow().chargeTypeHistoryId();
			case "CROSS_CONNECT_TYPE" -> crossConnectTypes.findCurrent(identityId)
					.orElseThrow()
					.crossConnectTypeHistoryId();
			case "MARKET_DATA_FEED" -> marketDataFeeds.findCurrent(identityId).orElseThrow().marketDataFeedHistoryId();
			case "MARKET_DATA_FEED_TYPE" -> marketDataFeedTypes.findCurrent(identityId)
					.orElseThrow()
					.marketDataFeedTypeHistoryId();
			case "FIRM" -> firms.findCurrent(identityId).orElseThrow().firmHistoryId();
			case "EXCHANGE" -> exchanges.findCurrent(identityId).orElseThrow().exchangeHistoryId();
			case "MARKET_SEGMENT" -> marketSegments.findCurrent(identityId).orElseThrow().marketSegmentHistoryId();
			case "USER" -> users.findCurrent(identityId).orElseThrow().userHistoryId();
			case "ASSET_TYPE" -> assetTypes.findCurrent(identityId).orElseThrow().assetTypeHistoryId();
			default -> throw new IllegalArgumentException("Unknown asset type: " + assetType);
		};
	}

	protected Long seedFirm(String name) {
		return applyAdd("FIRM", json(Map.of("firmName", name))).assetIdentityId();
	}

	protected Long seedExchange(String name, String type) {
		return applyAdd(
				"EXCHANGE",
				json(Map.of(
						"exchangeName", name,
						"exchangeCode", name,
						"exchangeAbbreviation", name,
						"exchangeType", type)))
				.assetIdentityId();
	}

	protected Long seedMarketSegment(String name, String type) {
		return applyAdd(
				"MARKET_SEGMENT",
				json(Map.of("marketSegmentName", name, "marketSegmentType", type)))
				.assetIdentityId();
	}

	protected Long seedUser(String name) {
		return applyAdd("USER", json(Map.of("userName", name))).assetIdentityId();
	}

	protected Long seedDataCenter(String name) {
		return applyAdd("DATA_CENTER", json(Map.of("dataCenterName", name))).assetIdentityId();
	}

	protected Long seedCage(String name, Long dataCenterId) {
		return applyAdd("CAGE", json(Map.of("cageName", name, "dataCenterId", dataCenterId)))
				.assetIdentityId();
	}

	protected Long seedRack(String name, Long cageId) {
		return applyAdd("RACK", json(Map.of("rackName", name, "cageId", cageId))).assetIdentityId();
	}

	protected Long seedRackDeviceType(String name, String kind) {
		return applyAdd(
				"RACK_DEVICE_TYPE",
				json(Map.of("rackDeviceTypeName", name, "rackDeviceTypeKind", kind)))
				.assetIdentityId();
	}

	protected Long seedRackDevicePortType(String name) {
		return applyAdd("RACK_DEVICE_PORT_TYPE", json(Map.of("rackDevicePortTypeName", name)))
				.assetIdentityId();
	}

	protected Long seedRackDevice(String name, Long rackId, Long rackDeviceTypeId) {
		return applyAdd(
				"RACK_DEVICE",
				json(Map.of(
						"rackDeviceName", name,
						"rackId", rackId,
						"rackDeviceTypeId", rackDeviceTypeId)))
				.assetIdentityId();
	}

	protected Long seedRackDevicePort(String name, Long rackDeviceId, Long rackDevicePortTypeId) {
		return applyAdd(
				"RACK_DEVICE_PORT",
				json(Map.of(
						"rackDevicePortName", name,
						"rackDeviceId", rackDeviceId,
						"rackDevicePortTypeId", rackDevicePortTypeId)))
				.assetIdentityId();
	}

	/** Fresh Data Center → Cage → Rack chain, each with a unique name. */
	protected Long seedRackInNewTree() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		return seedRack(unique("Rack"), cageId);
	}

	protected record SiteDeviceFixture(Long rackId, Long rackDeviceTypeId, Long rackDeviceId) {
	}

	/** Fresh device (with its own type) attached to a fresh rack. */
	protected SiteDeviceFixture seedDeviceInNewTree() {
		Long rackId = seedRackInNewTree();
		Long deviceTypeId = seedRackDeviceType(unique("DeviceType"), "EXTRANET_SWITCH");
		Long deviceId = seedRackDevice(unique("sw"), rackId, deviceTypeId);
		return new SiteDeviceFixture(rackId, deviceTypeId, deviceId);
	}

	protected record SitePortFixture(SiteDeviceFixture device, Long rackDevicePortTypeId, Long rackDevicePortId) {
	}

	/** Fresh port (with its own type) on a fresh device. */
	protected SitePortFixture seedPortInNewTree() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		return new SitePortFixture(device, portTypeId, portId);
	}

	/** Two distinct ports on the same fresh device, handy for cable tests. */
	protected Long[] seedPortPair() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portA = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		Long portB = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		return new Long[] { portA, portB };
	}

	protected Long seedLatency(String name, String type) {
		return applyAdd("LATENCY", json(Map.of("latencyName", name, "latencyType", type)))
				.assetIdentityId();
	}

	protected Long seedSpeed(String name, String type) {
		return applyAdd("SPEED", json(Map.of("speedName", name, "speedType", type))).assetIdentityId();
	}

	protected Long seedChargeType(String name) {
		return applyAdd("CHARGE_TYPE", json(Map.of("chargeTypeName", name))).assetIdentityId();
	}

	protected Long seedCrossConnectType(String name) {
		return applyAdd("CROSS_CONNECT_TYPE", json(Map.of("crossConnectTypeName", name)))
				.assetIdentityId();
	}

	protected Long seedMarketDataFeedType(String name) {
		return applyAdd("MARKET_DATA_FEED_TYPE", json(Map.of("marketDataFeedTypeName", name)))
				.assetIdentityId();
	}

	/** Fresh owner/billing firms + cross connect type + latency (LL) + speed (1G). */
	protected record XcDeps(Long ownerFirmId, Long billingFirmId, Long crossConnectTypeId, Long latencyId, Long speedId) {
	}

	protected XcDeps seedXcDeps() {
		return new XcDeps(
				seedFirm(unique("Owner")),
				seedFirm(unique("Billing")),
				seedCrossConnectType(unique("XcType")),
				seedLatency(unique("Latency"), "LL"),
				seedSpeed(unique("Speed"), "1G"));
	}

	protected String xcPayload(String circuitId, XcDeps deps) {
		return json(fields(
				"crossConnectName", "XC-" + circuitId,
				"circuitId", circuitId,
				"crossConnectTypeId", deps.crossConnectTypeId(),
				"latencyId", deps.latencyId(),
				"speedId", deps.speedId(),
				"ownerFirmId", deps.ownerFirmId(),
				"billingFirmId", deps.billingFirmId()));
	}

	protected Long seedCrossConnect(String circuitId, XcDeps deps) {
		return applyAdd("CROSS_CONNECT", xcPayload(circuitId, deps)).assetIdentityId();
	}

	protected Long seedMarketDataFeed(String name, Long crossConnectId, Long feedTypeId, XcDeps deps) {
		return applyAdd(
				"MARKET_DATA_FEED",
				json(fields(
						"marketDataFeedName", name,
						"crossConnectId", crossConnectId,
						"marketDataFeedTypeId", feedTypeId,
						"ownerFirmId", deps.ownerFirmId(),
						"billingFirmId", deps.billingFirmId())))
				.assetIdentityId();
	}

	protected Long seedDocument(String name, Long crossConnectId) {
		return applyAdd(
				"DOCUMENT",
				json(Map.of("documentName", name, "crossConnectId", crossConnectId)))
				.assetIdentityId();
	}

	protected Long seedCable(String name, Long portAId, Long portBId) {
		return applyAdd(
				"CABLE",
				json(Map.of("cableName", name, "portAId", portAId, "portBId", portBId)))
				.assetIdentityId();
	}
}
