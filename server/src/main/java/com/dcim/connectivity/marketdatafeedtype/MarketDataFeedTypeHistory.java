package com.dcim.connectivity.marketdatafeedtype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_DATA_FEED_TYPE_HISTORY")
public class MarketDataFeedTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_DATA_FEED_TYPE_HISTORY_ID", nullable = false)
	private Long marketDataFeedTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_DATA_FEED_TYPE_ID", nullable = false)
	private MarketDataFeedTypeIdentity marketDataFeedTypeIdentity;

	@Column(name = "MARKET_DATA_FEED_TYPE_NAME", nullable = false, length = 100)
	private String marketDataFeedTypeName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CHARGE_TYPE_ID")
	private ChargeTypeIdentity chargeTypeIdentity;

	protected MarketDataFeedTypeHistory() {
	}

	public MarketDataFeedTypeHistory(
			MarketDataFeedTypeIdentity marketDataFeedTypeIdentity,
			String marketDataFeedTypeName,
			ChargeTypeIdentity chargeTypeIdentity,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.marketDataFeedTypeIdentity = marketDataFeedTypeIdentity;
		this.marketDataFeedTypeName = marketDataFeedTypeName;
		this.chargeTypeIdentity = chargeTypeIdentity;
	}

	public Long getMarketDataFeedTypeHistoryId() {
		return marketDataFeedTypeHistoryId;
	}

	public MarketDataFeedTypeIdentity getMarketDataFeedTypeIdentity() {
		return marketDataFeedTypeIdentity;
	}

	public Long getMarketDataFeedTypeId() {
		return marketDataFeedTypeIdentity.getMarketDataFeedTypeId();
	}

	public String getMarketDataFeedTypeName() {
		return marketDataFeedTypeName;
	}

	public ChargeTypeIdentity getChargeTypeIdentity() {
		return chargeTypeIdentity;
	}

	public Long getChargeTypeId() {
		return chargeTypeIdentity == null ? null : chargeTypeIdentity.getChargeTypeId();
	}
}
