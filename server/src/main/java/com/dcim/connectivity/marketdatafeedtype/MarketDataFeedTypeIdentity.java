package com.dcim.connectivity.marketdatafeedtype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_DATA_FEED_TYPE_IDENTITY")
public class MarketDataFeedTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_DATA_FEED_TYPE_ID", nullable = false)
	private Long marketDataFeedTypeId;

	public MarketDataFeedTypeIdentity() {
	}

	public Long getMarketDataFeedTypeId() {
		return marketDataFeedTypeId;
	}
}
