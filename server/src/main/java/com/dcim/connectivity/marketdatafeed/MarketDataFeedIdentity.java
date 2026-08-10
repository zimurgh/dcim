package com.dcim.connectivity.marketdatafeed;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_DATA_FEED_IDENTITY")
public class MarketDataFeedIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_DATA_FEED_ID", nullable = false)
	private Long marketDataFeedId;

	public MarketDataFeedIdentity() {
	}

	public Long getMarketDataFeedId() {
		return marketDataFeedId;
	}
}
