package com.dcim.site.rack;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.site.cage.CageIdentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_RACK_HISTORY")
public class RackHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_HISTORY_ID", nullable = false)
	private Long rackHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_ID", nullable = false)
	private RackIdentity rackIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CAGE_ID", nullable = false)
	private CageIdentity cageIdentity;

	@Column(name = "RACK_NAME", nullable = false, length = 50)
	private String rackName;

	protected RackHistory() {
	}

	public RackHistory(
			RackIdentity rackIdentity,
			CageIdentity cageIdentity,
			String rackName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			String appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.rackIdentity = rackIdentity;
		this.cageIdentity = cageIdentity;
		this.rackName = rackName;
	}

	public Long getRackHistoryId() {
		return rackHistoryId;
	}

	public RackIdentity getRackIdentity() {
		return rackIdentity;
	}

	public Long getRackId() {
		return rackIdentity.getRackId();
	}

	public CageIdentity getCageIdentity() {
		return cageIdentity;
	}

	public Long getCageId() {
		return cageIdentity.getCageId();
	}

	public String getRackName() {
		return rackName;
	}
}
