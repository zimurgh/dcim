package com.dcim.connectivity.marketdatafeed;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity
@Immutable
@Table(name = "V_MARKET_DATA_FEED")
public class MarketDataFeedView extends AuditHistory {

	@Id
	@Column(name = "MARKET_DATA_FEED_HISTORY_ID", nullable = false)
	private Long marketDataFeedHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_DATA_FEED_ID", nullable = false)
	private MarketDataFeedIdentity marketDataFeedIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID", nullable = false)
	private CrossConnectIdentity crossConnectIdentity;

	@Column(name = "CROSS_CONNECT_NAME", nullable = false, length = 100)
	private String crossConnectName;

	@Column(name = "MARKET_DATA_FEED_TYPE_ID", nullable = false)
	private Long marketDataFeedTypeId;

	@Column(name = "MARKET_DATA_FEED_TYPE_NAME", nullable = false, length = 100)
	private String marketDataFeedTypeName;

	@Column(name = "MARKET_DATA_FEED_NAME", nullable = false, length = 100)
	private String marketDataFeedName;

	@Column(name = "OWNER_FIRM_ID", nullable = false)
	private Long ownerFirmId;

	@Column(name = "OWNER_FIRM_NAME", nullable = false, length = 50)
	private String ownerFirmName;

	@Column(name = "BILLING_FIRM_ID", nullable = false)
	private Long billingFirmId;

	@Column(name = "BILLING_FIRM_NAME", nullable = false, length = 50)
	private String billingFirmName;

	@Column(name = "PROVIDER_FIRM_ID")
	private Long providerFirmId;

	@Column(name = "PROVIDER_FIRM_NAME", length = 50)
	private String providerFirmName;

	protected MarketDataFeedView() {
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

	public String getCrossConnectName() {
		return crossConnectName;
	}

	public Long getMarketDataFeedTypeId() {
		return marketDataFeedTypeId;
	}

	public String getMarketDataFeedTypeName() {
		return marketDataFeedTypeName;
	}

	public String getMarketDataFeedName() {
		return marketDataFeedName;
	}

	public Long getOwnerFirmId() {
		return ownerFirmId;
	}

	public String getOwnerFirmName() {
		return ownerFirmName;
	}

	public Long getBillingFirmId() {
		return billingFirmId;
	}

	public String getBillingFirmName() {
		return billingFirmName;
	}

	public Long getProviderFirmId() {
		return providerFirmId;
	}

	public String getProviderFirmName() {
		return providerFirmName;
	}
}
