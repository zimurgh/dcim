package com.dcim.connectivity.speed;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_SPEED_HISTORY")
public class SpeedHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SPEED_HISTORY_ID", nullable = false)
	private Long speedHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "SPEED_ID", nullable = false)
	private SpeedIdentity speedIdentity;

	@Column(name = "SPEED_NAME", nullable = false, length = 100)
	private String speedName;

	@Enumerated(EnumType.STRING)
	@Column(name = "SPEED_TYPE", nullable = false, length = 50)
	private SpeedType speedType;

	protected SpeedHistory() {
	}

	public SpeedHistory(
			SpeedIdentity speedIdentity,
			String speedName,
			SpeedType speedType,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.speedIdentity = speedIdentity;
		this.speedName = speedName;
		this.speedType = speedType;
	}

	public Long getSpeedHistoryId() {
		return speedHistoryId;
	}

	public SpeedIdentity getSpeedIdentity() {
		return speedIdentity;
	}

	public Long getSpeedId() {
		return speedIdentity.getSpeedId();
	}

	public String getSpeedName() {
		return speedName;
	}

	public SpeedType getSpeedType() {
		return speedType;
	}
}
