package com.dcim.organization.marketsegment;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_MARKET_SEGMENT_HISTORY")
public class MarketSegmentHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MARKET_SEGMENT_HISTORY_ID", nullable = false)
	private Long marketSegmentHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_SEGMENT_ID", nullable = false)
	private MarketSegmentIdentity marketSegmentIdentity;

	@Column(name = "MARKET_SEGMENT_NAME", nullable = false, length = 100)
	private String marketSegmentName;

	@Enumerated(EnumType.STRING)
	@Column(name = "MARKET_SEGMENT_TYPE", nullable = false, length = 50)
	private MarketSegmentType marketSegmentType;

	protected MarketSegmentHistory() {
	}

	public MarketSegmentHistory(
			MarketSegmentIdentity marketSegmentIdentity,
			String marketSegmentName,
			MarketSegmentType marketSegmentType,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.marketSegmentIdentity = marketSegmentIdentity;
		this.marketSegmentName = marketSegmentName;
		this.marketSegmentType = marketSegmentType;
	}

	public Long getMarketSegmentHistoryId() {
		return marketSegmentHistoryId;
	}

	public MarketSegmentIdentity getMarketSegmentIdentity() {
		return marketSegmentIdentity;
	}

	public Long getMarketSegmentId() {
		return marketSegmentIdentity.getMarketSegmentId();
	}

	public String getMarketSegmentName() {
		return marketSegmentName;
	}

	public MarketSegmentType getMarketSegmentType() {
		return marketSegmentType;
	}
}
