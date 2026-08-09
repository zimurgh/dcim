package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Shared temporal / audit columns for append-only asset history rows.
 */
@MappedSuperclass
public abstract class AuditHistory {

	@Column(name = "VALID_FROM", nullable = false)
	private LocalDate validFrom;

	@Column(name = "VALID_TO")
	private LocalDate validTo;

	@Column(name = "APPLIED_AT", nullable = false)
	private Instant appliedAt;

	@Column(name = "APPLIED_BY", length = 50)
	private String appliedBy;

	@Column(name = "ACTION", length = 50)
	private String action;

	@Column(name = "STATUS", length = 50)
	private String status;

	protected AuditHistory() {
	}

	protected AuditHistory(
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			String appliedBy,
			String action,
			String status) {
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.appliedAt = appliedAt;
		this.appliedBy = appliedBy;
		this.action = action;
		this.status = status;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public Instant getAppliedAt() {
		return appliedAt;
	}

	public String getAppliedBy() {
		return appliedBy;
	}

	public String getAction() {
		return action;
	}

	public String getStatus() {
		return status;
	}

	public boolean isCurrent() {
		return validTo == null;
	}
}
