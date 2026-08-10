package com.dcim.organization.marketsegment;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_SEGMENT_IDENTITY")
public class MarketSegmentIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_SEGMENT_ID", nullable = false)
	private Long marketSegmentId;

	public MarketSegmentIdentity() {
	}

	public Long getMarketSegmentId() {
		return marketSegmentId;
	}
}
