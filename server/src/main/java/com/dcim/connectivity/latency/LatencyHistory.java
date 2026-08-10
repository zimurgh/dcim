package com.dcim.connectivity.latency;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_LATENCY_HISTORY")
public class LatencyHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "LATENCY_HISTORY_ID", nullable = false)
	private Long latencyHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "LATENCY_ID", nullable = false)
	private LatencyIdentity latencyIdentity;

	@Column(name = "LATENCY_NAME", nullable = false, length = 100)
	private String latencyName;

	@Enumerated(EnumType.STRING)
	@Column(name = "LATENCY_TYPE", nullable = false, length = 50)
	private LatencyType latencyType;

	protected LatencyHistory() {
	}

	public LatencyHistory(
			LatencyIdentity latencyIdentity,
			String latencyName,
			LatencyType latencyType,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.latencyIdentity = latencyIdentity;
		this.latencyName = latencyName;
		this.latencyType = latencyType;
	}

	public Long getLatencyHistoryId() {
		return latencyHistoryId;
	}

	public LatencyIdentity getLatencyIdentity() {
		return latencyIdentity;
	}

	public Long getLatencyId() {
		return latencyIdentity.getLatencyId();
	}

	public String getLatencyName() {
		return latencyName;
	}

	public LatencyType getLatencyType() {
		return latencyType;
	}
}
