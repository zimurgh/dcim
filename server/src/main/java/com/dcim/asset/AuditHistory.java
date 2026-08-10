package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

/**
 * Shared temporal / audit columns for append-only asset history rows.
 * {@code appliedBy} is the applying user's stable id ({@code T_USER_IDENTITY.USER_ID}).
 */
@MappedSuperclass
public abstract class AuditHistory {

	@Column(name = "VALID_FROM", nullable = false)
	private LocalDate validFrom;

	@Column(name = "VALID_TO")
	private LocalDate validTo;

	@Column(name = "APPLIED_AT", nullable = false)
	private Instant appliedAt;

	@Column(name = "APPLIED_BY")
	private Long appliedBy;

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
			Long appliedBy,
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

	public Long getAppliedBy() {
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

	/**
	 * Closes this history row as of {@code validTo}. Append-only: never clears a prior close.
	 */
	public void close(LocalDate validTo) {
		if (validTo == null) {
			throw new IllegalArgumentException("validTo is required to close history");
		}
		if (this.validTo != null) {
			throw new IllegalStateException("History row is already closed");
		}
		this.validTo = validTo;
	}
}
