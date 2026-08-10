package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.connectivity.cable.CableDto;
import com.dcim.connectivity.cable.CableService;
import com.dcim.connectivity.chargetype.ChargeTypeDto;
import com.dcim.connectivity.chargetype.ChargeTypeService;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeDto;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeService;
import com.dcim.connectivity.latency.LatencyDto;
import com.dcim.connectivity.latency.LatencyService;
import com.dcim.connectivity.latency.LatencyType;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedDto;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedService;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeDto;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeService;
import com.dcim.connectivity.speed.SpeedDto;
import com.dcim.connectivity.speed.SpeedService;
import com.dcim.connectivity.speed.SpeedType;
import com.dcim.organization.exchange.ExchangeDto;
import com.dcim.organization.exchange.ExchangeService;
import com.dcim.organization.exchange.ExchangeType;
import com.dcim.organization.firm.FirmDto;
import com.dcim.organization.firm.FirmService;
import com.dcim.organization.marketsegment.MarketSegmentDto;
import com.dcim.organization.marketsegment.MarketSegmentService;
import com.dcim.organization.marketsegment.MarketSegmentType;
import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
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
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeDto;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeService;
import com.dcim.site.rackdevicetype.RackDeviceTypeDto;
import com.dcim.site.rackdevicetype.RackDeviceTypeKind;
import com.dcim.site.rackdevicetype.RackDeviceTypeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
	ExchangeService exchanges;

	@Autowired
	MarketSegmentService marketSegments;

	@Autowired
	DataCenterService dataCenters;

	@Autowired
	CageService cages;

	@Autowired
	RackService racks;

	@Autowired
	RackDeviceTypeService rackDeviceTypes;

	@Autowired
	RackDeviceService devices;

	@Autowired
	RackDevicePortTypeService rackDevicePortTypes;

	@Autowired
	RackDevicePortService ports;

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
	CableService cables;

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
	void exchangeAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.EXCHANGE,
				ChangeAction.ADD,
				null,
				null,
				"{\"exchangeName\":\"Chicago Board Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"CBOE\",\"exchangeType\":\"OPTIONS\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.EXCHANGE);

		ExchangeDto current = exchanges.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.exchangeName()).isEqualTo("Chicago Board Options Exchange");
		assertThat(current.exchangeCode()).isEqualTo("CBOE");
		assertThat(current.exchangeAbbreviation()).isEqualTo("CBOE");
		assertThat(current.exchangeType()).isEqualTo(ExchangeType.OPTIONS);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(exchanges.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.EXCHANGE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.exchangeHistoryId(),
				"{\"exchangeName\":\"Cboe Options Exchange\",\"exchangeCode\":\"CBOE\","
						+ "\"exchangeAbbreviation\":\"Cboe\",\"exchangeType\":\"OPTIONS\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		ExchangeDto afterUpdate = exchanges.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.exchangeName()).isEqualTo("Cboe Options Exchange");
		assertThat(afterUpdate.exchangeAbbreviation()).isEqualTo("Cboe");
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(exchanges.history(add.assetIdentityId())).hasSize(2);
		assertThat(exchanges.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.EXCHANGE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.exchangeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		ExchangeDto afterTerminate = exchanges.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.exchangeCode()).isEqualTo("CBOE");
		assertThat(exchanges.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void marketSegmentAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.MARKET_SEGMENT,
				ChangeAction.ADD,
				null,
				null,
				"{\"marketSegmentName\":\"Equities Index\",\"marketSegmentType\":\"Equities Index\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.MARKET_SEGMENT);

		MarketSegmentDto current = marketSegments.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.marketSegmentName()).isEqualTo("Equities Index");
		assertThat(current.marketSegmentType()).isEqualTo(MarketSegmentType.EQUITIES_INDEX);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(marketSegments.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.MARKET_SEGMENT,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.marketSegmentHistoryId(),
				"{\"marketSegmentName\":\"Agricultural Futures\","
						+ "\"marketSegmentType\":\"AGRICULTURAL_FUTURES\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		MarketSegmentDto afterUpdate = marketSegments.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.marketSegmentName()).isEqualTo("Agricultural Futures");
		assertThat(afterUpdate.marketSegmentType()).isEqualTo(MarketSegmentType.AGRICULTURAL_FUTURES);
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(marketSegments.history(add.assetIdentityId())).hasSize(2);
		assertThat(marketSegments.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.MARKET_SEGMENT,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.marketSegmentHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		MarketSegmentDto afterTerminate = marketSegments.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.marketSegmentType()).isEqualTo(MarketSegmentType.AGRICULTURAL_FUTURES);
		assertThat(marketSegments.history(add.assetIdentityId())).hasSize(3);
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
	void rackDeviceTypeAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.RACK_DEVICE_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceTypeName\":\"Extranet Switch\",\"rackDeviceTypeKind\":\"EXTRANET_SWITCH\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.RACK_DEVICE_TYPE);

		RackDeviceTypeDto current = rackDeviceTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceTypeName()).isEqualTo("Extranet Switch");
		assertThat(current.rackDeviceTypeKind()).isEqualTo(RackDeviceTypeKind.EXTRANET_SWITCH);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(rackDeviceTypes.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.RACK_DEVICE_TYPE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.rackDeviceTypeHistoryId(),
				"{\"rackDeviceTypeName\":\"Matrix Switch\",\"rackDeviceTypeKind\":\"Matrix Switch\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDeviceTypeDto afterUpdate = rackDeviceTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.rackDeviceTypeName()).isEqualTo("Matrix Switch");
		assertThat(afterUpdate.rackDeviceTypeKind()).isEqualTo(RackDeviceTypeKind.MATRIX_SWITCH);
		assertThat(rackDeviceTypes.history(add.assetIdentityId())).hasSize(2);
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.RACK_DEVICE_TYPE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.rackDeviceTypeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		RackDeviceTypeDto afterTerminate = rackDeviceTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.rackDeviceTypeKind()).isEqualTo(RackDeviceTypeKind.MATRIX_SWITCH);
		assertThat(rackDeviceTypes.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void rackDevicePortTypeAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.RACK_DEVICE_PORT_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortTypeName\":\"Copper\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.RACK_DEVICE_PORT_TYPE);

		RackDevicePortTypeDto current = rackDevicePortTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortTypeName()).isEqualTo("Copper");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(rackDevicePortTypes.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.RACK_DEVICE_PORT_TYPE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.rackDevicePortTypeHistoryId(),
				"{\"rackDevicePortTypeName\":\"Fiber\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		RackDevicePortTypeDto afterUpdate = rackDevicePortTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.rackDevicePortTypeName()).isEqualTo("Fiber");
		assertThat(rackDevicePortTypes.history(add.assetIdentityId())).hasSize(2);
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.RACK_DEVICE_PORT_TYPE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.rackDevicePortTypeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		RackDevicePortTypeDto afterTerminate = rackDevicePortTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.rackDevicePortTypeName()).isEqualTo("Fiber");
		assertThat(rackDevicePortTypes.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void rackDeviceAddThroughStagesWritesHistory() {
		Long rackId = seedRack();
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");
		ChangeDto applied = progress(
				AssetType.RACK_DEVICE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}");
		RackDeviceDto current = devices.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw1");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(devices.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.RACK_DEVICE);
	}

	@Test
	void rackDevicePortAddThroughStagesWritesHistory() {
		Long rackId = seedRack();
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");
		Long portTypeId = seedRackDevicePortType("Copper");
		Long deviceId = progress(
				AssetType.RACK_DEVICE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}")
				.assetIdentityId();

		ChangeDto applied = progress(
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}");
		RackDevicePortDto current = ports.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(deviceId);
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(ports.history(applied.assetIdentityId())).hasSize(1);
		assertCreatedLink(applied, AssetType.RACK_DEVICE_PORT);
	}

	@Test
	void latencyAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.LATENCY,
				ChangeAction.ADD,
				null,
				null,
				"{\"latencyName\":\"Low Latency\",\"latencyType\":\"LL\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.LATENCY);

		LatencyDto current = latencies.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.latencyName()).isEqualTo("Low Latency");
		assertThat(current.latencyType()).isEqualTo(LatencyType.LL);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(latencies.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.LATENCY,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.latencyHistoryId(),
				"{\"latencyName\":\"Ultra Low Latency\",\"latencyType\":\"ULL\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		LatencyDto afterUpdate = latencies.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.latencyName()).isEqualTo("Ultra Low Latency");
		assertThat(afterUpdate.latencyType()).isEqualTo(LatencyType.ULL);
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(latencies.history(add.assetIdentityId())).hasSize(2);
		assertThat(latencies.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.LATENCY,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.latencyHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		LatencyDto afterTerminate = latencies.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.latencyType()).isEqualTo(LatencyType.ULL);
		assertThat(latencies.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void speedAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.SPEED,
				ChangeAction.ADD,
				null,
				null,
				"{\"speedName\":\"1 Gigabit\",\"speedType\":\"1G\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.SPEED);

		SpeedDto current = speeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.speedName()).isEqualTo("1 Gigabit");
		assertThat(current.speedType()).isEqualTo(SpeedType.ONE_G);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(speeds.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.SPEED,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.speedHistoryId(),
				"{\"speedName\":\"10 Gigabit\",\"speedType\":\"10G\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		SpeedDto afterUpdate = speeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.speedName()).isEqualTo("10 Gigabit");
		assertThat(afterUpdate.speedType()).isEqualTo(SpeedType.TEN_G);
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(speeds.history(add.assetIdentityId())).hasSize(2);
		assertThat(speeds.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.SPEED,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.speedHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		SpeedDto afterTerminate = speeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.speedType()).isEqualTo(SpeedType.TEN_G);
		assertThat(speeds.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void crossConnectAddUpdateTerminateThroughStages() {
		Long ownerFirmId = seedFirm("OwnerCo");
		Long billingFirmId = seedFirm("BillingCo");
		Long providerFirmId = seedFirm("ProviderCo");
		Long newBillingFirmId = seedFirm("BillingCo2");
		Long crossConnectTypeId = seedCrossConnectType("Single Mode Fiber");
		Long otherCrossConnectTypeId = seedCrossConnectType("Dark Fiber");
		Long latencyLlId = seedLatency("Low Latency", "LL");
		Long latencyUllId = seedLatency("Ultra Low Latency", "ULL");
		Long speed1gId = seedSpeed("1 Gigabit", "1G");
		Long speed10gId = seedSpeed("10 Gigabit", "10G");
		Long marketSegmentId = seedMarketSegment("Equities Index", "Equities Index");

		ChangeDto add = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectName\":\"XC-1\",\"circuitId\":\"CKT-001\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyLlId
						+ ",\"speedId\":" + speed1gId
						+ ",\"marketSegmentId\":" + marketSegmentId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId
						+ ",\"providerFirmId\":" + providerFirmId + "}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.CROSS_CONNECT);

		CrossConnectDto current = crossConnects.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectName()).isEqualTo("XC-1");
		assertThat(current.circuitId()).isEqualTo("CKT-001");
		assertThat(current.crossConnectTypeId()).isEqualTo(crossConnectTypeId);
		assertThat(current.latencyId()).isEqualTo(latencyLlId);
		assertThat(current.speedId()).isEqualTo(speed1gId);
		assertThat(current.marketSegmentId()).isEqualTo(marketSegmentId);
		assertThat(current.ownerFirmId()).isEqualTo(ownerFirmId);
		assertThat(current.billingFirmId()).isEqualTo(billingFirmId);
		assertThat(current.providerFirmId()).isEqualTo(providerFirmId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(crossConnects.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.crossConnectHistoryId(),
				"{\"crossConnectName\":\"XC-1-REN\",\"circuitId\":\"CKT-001B\",\"crossConnectTypeId\":"
						+ otherCrossConnectTypeId
						+ ",\"latencyId\":" + latencyUllId
						+ ",\"speedId\":" + speed10gId
						+ ",\"marketSegmentId\":null"
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + newBillingFirmId
						+ ",\"providerFirmId\":null}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CrossConnectDto afterUpdate = crossConnects.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.crossConnectName()).isEqualTo("XC-1-REN");
		assertThat(afterUpdate.circuitId()).isEqualTo("CKT-001B");
		assertThat(afterUpdate.crossConnectTypeId()).isEqualTo(otherCrossConnectTypeId);
		assertThat(afterUpdate.latencyId()).isEqualTo(latencyUllId);
		assertThat(afterUpdate.speedId()).isEqualTo(speed10gId);
		assertThat(afterUpdate.marketSegmentId()).isNull();
		assertThat(afterUpdate.billingFirmId()).isEqualTo(newBillingFirmId);
		assertThat(afterUpdate.providerFirmId()).isNull();
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(crossConnects.history(add.assetIdentityId())).hasSize(2);
		assertThat(crossConnects.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.crossConnectHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		CrossConnectDto afterTerminate = crossConnects.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.crossConnectName()).isEqualTo("XC-1-REN");
		assertThat(afterTerminate.circuitId()).isEqualTo("CKT-001B");
		assertThat(afterTerminate.crossConnectTypeId()).isEqualTo(otherCrossConnectTypeId);
		assertThat(afterTerminate.latencyId()).isEqualTo(latencyUllId);
		assertThat(afterTerminate.speedId()).isEqualTo(speed10gId);
		assertThat(afterTerminate.marketSegmentId()).isNull();
		assertThat(crossConnects.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void chargeTypeAddUpdateTerminateThroughStages() {
		ChangeDto add = progress(
				AssetType.CHARGE_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"chargeTypeName\":\"MRC\"}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.CHARGE_TYPE);

		ChargeTypeDto current = chargeTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.chargeTypeName()).isEqualTo("MRC");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(chargeTypes.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.CHARGE_TYPE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.chargeTypeHistoryId(),
				"{\"chargeTypeName\":\"NRC\"}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		ChargeTypeDto afterUpdate = chargeTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.chargeTypeName()).isEqualTo("NRC");
		assertThat(chargeTypes.history(add.assetIdentityId())).hasSize(2);
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.CHARGE_TYPE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.chargeTypeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		ChargeTypeDto afterTerminate = chargeTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.chargeTypeName()).isEqualTo("NRC");
		assertThat(chargeTypes.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void crossConnectTypeAddUpdateTerminateThroughStages() {
		Long chargeTypeId = seedChargeType("MRC");
		ChangeDto add = progress(
				AssetType.CROSS_CONNECT_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectTypeName\":\"Single Mode Fiber\",\"chargeTypeId\":" + chargeTypeId + "}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.CROSS_CONNECT_TYPE);

		CrossConnectTypeDto current = crossConnectTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectTypeName()).isEqualTo("Single Mode Fiber");
		assertThat(current.chargeTypeId()).isEqualTo(chargeTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(crossConnectTypes.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.CROSS_CONNECT_TYPE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.crossConnectTypeHistoryId(),
				"{\"crossConnectTypeName\":\"Dark Fiber\",\"chargeTypeId\":null}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CrossConnectTypeDto afterUpdate = crossConnectTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.crossConnectTypeName()).isEqualTo("Dark Fiber");
		assertThat(afterUpdate.chargeTypeId()).isNull();
		assertThat(crossConnectTypes.history(add.assetIdentityId())).hasSize(2);
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.CROSS_CONNECT_TYPE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.crossConnectTypeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		CrossConnectTypeDto afterTerminate = crossConnectTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.crossConnectTypeName()).isEqualTo("Dark Fiber");
		assertThat(afterTerminate.chargeTypeId()).isNull();
		assertThat(crossConnectTypes.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void marketDataFeedTypeAddUpdateTerminateThroughStages() {
		Long chargeTypeId = seedChargeType("Feed MRC");
		ChangeDto add = progress(
				AssetType.MARKET_DATA_FEED_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"marketDataFeedTypeName\":\"Depth\",\"chargeTypeId\":" + chargeTypeId + "}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.MARKET_DATA_FEED_TYPE);

		MarketDataFeedTypeDto current = marketDataFeedTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedTypeName()).isEqualTo("Depth");
		assertThat(current.chargeTypeId()).isEqualTo(chargeTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(marketDataFeedTypes.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.MARKET_DATA_FEED_TYPE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.marketDataFeedTypeHistoryId(),
				"{\"marketDataFeedTypeName\":\"Top of Book\",\"chargeTypeId\":null}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		MarketDataFeedTypeDto afterUpdate = marketDataFeedTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.marketDataFeedTypeName()).isEqualTo("Top of Book");
		assertThat(afterUpdate.chargeTypeId()).isNull();
		assertThat(marketDataFeedTypes.history(add.assetIdentityId())).hasSize(2);
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.MARKET_DATA_FEED_TYPE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.marketDataFeedTypeHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		MarketDataFeedTypeDto afterTerminate = marketDataFeedTypes.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.marketDataFeedTypeName()).isEqualTo("Top of Book");
		assertThat(afterTerminate.chargeTypeId()).isNull();
		assertThat(marketDataFeedTypes.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void marketDataFeedAddUpdateTerminateThroughStages() {
		Long ownerFirmId = seedFirm("OwnerCo");
		Long billingFirmId = seedFirm("BillingCo");
		Long providerFirmId = seedFirm("ProviderCo");
		Long latencyId = seedLatency("Low Latency", "LL");
		Long speedId = seedSpeed("1 Gigabit", "1G");
		Long feedTypeId = seedMarketDataFeedType("Depth");
		Long otherFeedTypeId = seedMarketDataFeedType("Top of Book");
		Long crossConnectTypeId = seedCrossConnectType("Single Mode Fiber");
		Long crossConnectId = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectName\":\"XC-1\",\"circuitId\":\"CKT-XC-1\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}")
				.assetIdentityId();
		Long otherCrossConnectId = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectName\":\"XC-2\",\"circuitId\":\"CKT-XC-2\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}")
				.assetIdentityId();

		ChangeDto add = progress(
				AssetType.MARKET_DATA_FEED,
				ChangeAction.ADD,
				null,
				null,
				"{\"marketDataFeedName\":\"FEED-A\",\"crossConnectId\":" + crossConnectId
						+ ",\"marketDataFeedTypeId\":" + feedTypeId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.MARKET_DATA_FEED);

		MarketDataFeedDto current = marketDataFeeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A");
		assertThat(current.crossConnectId()).isEqualTo(crossConnectId);
		assertThat(current.marketDataFeedTypeId()).isEqualTo(feedTypeId);
		assertThat(current.ownerFirmId()).isEqualTo(ownerFirmId);
		assertThat(current.billingFirmId()).isEqualTo(billingFirmId);
		assertThat(current.providerFirmId()).isNull();
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(marketDataFeeds.listCurrentByCrossConnect(crossConnectId)).hasSize(1);
		assertThat(marketDataFeeds.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.MARKET_DATA_FEED,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.marketDataFeedHistoryId(),
				"{\"marketDataFeedName\":\"FEED-A2\",\"crossConnectId\":" + otherCrossConnectId
						+ ",\"marketDataFeedTypeId\":" + otherFeedTypeId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId
						+ ",\"providerFirmId\":" + providerFirmId + "}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		MarketDataFeedDto afterUpdate = marketDataFeeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.marketDataFeedName()).isEqualTo("FEED-A2");
		assertThat(afterUpdate.crossConnectId()).isEqualTo(otherCrossConnectId);
		assertThat(afterUpdate.marketDataFeedTypeId()).isEqualTo(otherFeedTypeId);
		assertThat(afterUpdate.providerFirmId()).isEqualTo(providerFirmId);
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(marketDataFeeds.listCurrentByCrossConnect(crossConnectId)).isEmpty();
		assertThat(marketDataFeeds.listCurrentByCrossConnect(otherCrossConnectId)).hasSize(1);
		assertThat(marketDataFeeds.history(add.assetIdentityId())).hasSize(2);
		assertThat(marketDataFeeds.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.MARKET_DATA_FEED,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.marketDataFeedHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		MarketDataFeedDto afterTerminate = marketDataFeeds.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.marketDataFeedName()).isEqualTo("FEED-A2");
		assertThat(afterTerminate.marketDataFeedTypeId()).isEqualTo(otherFeedTypeId);
		assertThat(marketDataFeeds.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	@Test
	void cableAddUpdateTerminateThroughStages() {
		Long ownerFirmId = seedFirm("OwnerCo");
		Long billingFirmId = seedFirm("BillingCo");
		Long latencyId = seedLatency("Low Latency", "LL");
		Long speedId = seedSpeed("1 Gigabit", "1G");
		Long crossConnectTypeId = seedCrossConnectType("Single Mode Fiber");
		Long crossConnectId = progress(
				AssetType.CROSS_CONNECT,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectName\":\"XC-1\",\"circuitId\":\"CKT-XC-1\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}")
				.assetIdentityId();
		Long[] portIds = seedTwoPorts();
		Long sparePortId = progress(
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortName\":\"eth2\",\"rackDeviceId\":"
						+ ports.findCurrent(portIds[0]).orElseThrow().rackDeviceId()
						+ ",\"rackDevicePortTypeId\":"
						+ ports.findCurrent(portIds[0]).orElseThrow().rackDevicePortTypeId() + "}")
				.assetIdentityId();

		ChangeDto add = progress(
				AssetType.CABLE,
				ChangeAction.ADD,
				null,
				null,
				"{\"cableName\":\"CBL-1\",\"portAId\":" + portIds[0]
						+ ",\"portBId\":" + portIds[1]
						+ ",\"crossConnectId\":" + crossConnectId + "}");
		assertThat(add.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(add.statusLabel()).isEqualTo("Active");
		assertCreatedLink(add, AssetType.CABLE);

		CableDto current = cables.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(current.cableName()).isEqualTo("CBL-1");
		assertThat(current.portAId()).isEqualTo(portIds[0]);
		assertThat(current.portBId()).isEqualTo(portIds[1]);
		assertThat(current.crossConnectId()).isEqualTo(crossConnectId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(cables.listCurrentByCrossConnect(crossConnectId)).hasSize(1);
		assertThat(cables.history(add.assetIdentityId())).hasSize(1);

		ChangeDto update = progress(
				AssetType.CABLE,
				ChangeAction.UPDATE,
				add.assetIdentityId(),
				current.cableHistoryId(),
				"{\"cableName\":\"CBL-1B\",\"portAId\":" + portIds[0]
						+ ",\"portBId\":" + sparePortId
						+ ",\"crossConnectId\":null}");
		assertThat(update.statusLabel()).isEqualTo("Active");
		assertThat(update.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CableDto afterUpdate = cables.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterUpdate.cableName()).isEqualTo("CBL-1B");
		assertThat(afterUpdate.portBId()).isEqualTo(sparePortId);
		assertThat(afterUpdate.crossConnectId()).isNull();
		assertThat(afterUpdate.action()).isEqualTo("UPDATE");
		assertThat(cables.listCurrentByCrossConnect(crossConnectId)).isEmpty();
		assertThat(cables.history(add.assetIdentityId())).hasSize(2);
		assertThat(cables.history(add.assetIdentityId()).getFirst().validTo()).isNotNull();
		assertCommittedLinks(update.changeId(), 2);

		ChangeDto terminate = progress(
				AssetType.CABLE,
				ChangeAction.TERMINATE,
				add.assetIdentityId(),
				afterUpdate.cableHistoryId(),
				"{}");
		assertThat(terminate.statusLabel()).isEqualTo("Terminated");
		CableDto afterTerminate = cables.findCurrent(add.assetIdentityId()).orElseThrow();
		assertThat(afterTerminate.status()).isEqualTo("Terminated");
		assertThat(afterTerminate.action()).isEqualTo("TERMINATE");
		assertThat(afterTerminate.cableName()).isEqualTo("CBL-1B");
		assertThat(cables.history(add.assetIdentityId())).hasSize(3);
		assertCommittedLinks(terminate.changeId(), 2);
	}

	private Long seedFirm(String firmName) {
		return progress(
				AssetType.FIRM,
				ChangeAction.ADD,
				null,
				null,
				"{\"firmName\":\"" + firmName + "\"}")
				.assetIdentityId();
	}

	private Long seedLatency(String latencyName, String latencyType) {
		return progress(
				AssetType.LATENCY,
				ChangeAction.ADD,
				null,
				null,
				"{\"latencyName\":\"" + latencyName + "\",\"latencyType\":\"" + latencyType + "\"}")
				.assetIdentityId();
	}

	private Long seedSpeed(String speedName, String speedType) {
		return progress(
				AssetType.SPEED,
				ChangeAction.ADD,
				null,
				null,
				"{\"speedName\":\"" + speedName + "\",\"speedType\":\"" + speedType + "\"}")
				.assetIdentityId();
	}

	private Long seedMarketSegment(String marketSegmentName, String marketSegmentType) {
		return progress(
				AssetType.MARKET_SEGMENT,
				ChangeAction.ADD,
				null,
				null,
				"{\"marketSegmentName\":\"" + marketSegmentName
						+ "\",\"marketSegmentType\":\"" + marketSegmentType + "\"}")
				.assetIdentityId();
	}

	private Long seedMarketDataFeedType(String marketDataFeedTypeName) {
		return progress(
				AssetType.MARKET_DATA_FEED_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"marketDataFeedTypeName\":\"" + marketDataFeedTypeName + "\"}")
				.assetIdentityId();
	}

	private Long seedCrossConnectType(String crossConnectTypeName) {
		return progress(
				AssetType.CROSS_CONNECT_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"crossConnectTypeName\":\"" + crossConnectTypeName + "\"}")
				.assetIdentityId();
	}

	private Long seedChargeType(String chargeTypeName) {
		return progress(
				AssetType.CHARGE_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"chargeTypeName\":\"" + chargeTypeName + "\"}")
				.assetIdentityId();
	}

	private Long seedRackDeviceType(String name, String kind) {
		return progress(
				AssetType.RACK_DEVICE_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceTypeName\":\"" + name + "\",\"rackDeviceTypeKind\":\"" + kind + "\"}")
				.assetIdentityId();
	}

	private Long seedRackDevicePortType(String name) {
		return progress(
				AssetType.RACK_DEVICE_PORT_TYPE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortTypeName\":\"" + name + "\"}")
				.assetIdentityId();
	}

	private Long[] seedTwoPorts() {
		Long rackId = seedRack();
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");
		Long portTypeId = seedRackDevicePortType("Copper");
		Long deviceId = progress(
				AssetType.RACK_DEVICE,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rackId
						+ ",\"rackDeviceTypeId\":" + deviceTypeId + "}")
				.assetIdentityId();
		Long portA = progress(
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortName\":\"eth0\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}")
				.assetIdentityId();
		Long portB = progress(
				AssetType.RACK_DEVICE_PORT,
				ChangeAction.ADD,
				null,
				null,
				"{\"rackDevicePortName\":\"eth1\",\"rackDeviceId\":" + deviceId
						+ ",\"rackDevicePortTypeId\":" + portTypeId + "}")
				.assetIdentityId();
		return new Long[] { portA, portB };
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

		ChangeDto committed = changes.applyStaged(untracked.changeId(), appliedBy);
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
