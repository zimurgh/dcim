package com.dcim.connectivity.marketdatafeed;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeIdentity;
import com.dcim.organization.firm.FirmIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_DATA_FEED_HISTORY")
public class MarketDataFeedHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_DATA_FEED_HISTORY_ID", nullable = false)
	private Long marketDataFeedHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_DATA_FEED_ID", nullable = false)
	private MarketDataFeedIdentity marketDataFeedIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID", nullable = false)
	private CrossConnectIdentity crossConnectIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_DATA_FEED_TYPE_ID", nullable = false)
	private MarketDataFeedTypeIdentity marketDataFeedTypeIdentity;

	@Column(name = "MARKET_DATA_FEED_NAME", nullable = false, length = 100)
	private String marketDataFeedName;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "OWNER_FIRM_ID", nullable = false)
	private FirmIdentity ownerFirmIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "BILLING_FIRM_ID", nullable = false)
	private FirmIdentity billingFirmIdentity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROVIDER_FIRM_ID")
	private FirmIdentity providerFirmIdentity;

	protected MarketDataFeedHistory() {
	}

	public MarketDataFeedHistory(
			MarketDataFeedIdentity marketDataFeedIdentity,
			CrossConnectIdentity crossConnectIdentity,
			MarketDataFeedTypeIdentity marketDataFeedTypeIdentity,
			String marketDataFeedName,
			FirmIdentity ownerFirmIdentity,
			FirmIdentity billingFirmIdentity,
			FirmIdentity providerFirmIdentity,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.marketDataFeedIdentity = marketDataFeedIdentity;
		this.crossConnectIdentity = crossConnectIdentity;
		this.marketDataFeedTypeIdentity = marketDataFeedTypeIdentity;
		this.marketDataFeedName = marketDataFeedName;
		this.ownerFirmIdentity = ownerFirmIdentity;
		this.billingFirmIdentity = billingFirmIdentity;
		this.providerFirmIdentity = providerFirmIdentity;
	}

	public Long getMarketDataFeedHistoryId() {
		return marketDataFeedHistoryId;
	}

	public MarketDataFeedIdentity getMarketDataFeedIdentity() {
		return marketDataFeedIdentity;
	}

	public Long getMarketDataFeedId() {
		return marketDataFeedIdentity.getMarketDataFeedId();
	}

	public CrossConnectIdentity getCrossConnectIdentity() {
		return crossConnectIdentity;
	}

	public Long getCrossConnectId() {
		return crossConnectIdentity.getCrossConnectId();
	}

	public MarketDataFeedTypeIdentity getMarketDataFeedTypeIdentity() {
		return marketDataFeedTypeIdentity;
	}

	public Long getMarketDataFeedTypeId() {
		return marketDataFeedTypeIdentity.getMarketDataFeedTypeId();
	}

	public String getMarketDataFeedName() {
		return marketDataFeedName;
	}

	public FirmIdentity getOwnerFirmIdentity() {
		return ownerFirmIdentity;
	}

	public Long getOwnerFirmId() {
		return ownerFirmIdentity.getFirmId();
	}

	public FirmIdentity getBillingFirmIdentity() {
		return billingFirmIdentity;
	}

	public Long getBillingFirmId() {
		return billingFirmIdentity.getFirmId();
	}

	public FirmIdentity getProviderFirmIdentity() {
		return providerFirmIdentity;
	}

	public Long getProviderFirmId() {
		return providerFirmIdentity == null ? null : providerFirmIdentity.getFirmId();
	}
}
