package com.dcim.organization.firm;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_FIRM_HISTORY")
public class FirmHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "FIRM_HISTORY_ID", nullable = false)
	private Long firmHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "FIRM_ID", nullable = false)
	private FirmIdentity firmIdentity;

	@Column(name = "FIRM_NAME", nullable = false, length = 50)
	private String firmName;

	@Column(name = "PARENT_FIRM_NAME", length = 50)
	private String parentFirmName;

	protected FirmHistory() {
	}

	public FirmHistory(
			FirmIdentity firmIdentity,
			String firmName,
			String parentFirmName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			String appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.firmIdentity = firmIdentity;
		this.firmName = firmName;
		this.parentFirmName = parentFirmName;
	}

	public Long getFirmHistoryId() {
		return firmHistoryId;
	}

	public FirmIdentity getFirmIdentity() {
		return firmIdentity;
	}

	public Long getFirmId() {
		return firmIdentity.getFirmId();
	}

	public String getFirmName() {
		return firmName;
	}

	public String getParentFirmName() {
		return parentFirmName;
	}
}
