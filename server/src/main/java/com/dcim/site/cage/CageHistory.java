package com.dcim.site.cage;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.site.datacenter.DataCenterIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CAGE_HISTORY")
public class CageHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CAGE_HISTORY_ID", nullable = false)
	private Long cageHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CAGE_ID", nullable = false)
	private CageIdentity cageIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "DATA_CENTER_ID", nullable = false)
	private DataCenterIdentity dataCenterIdentity;

	@Column(name = "CAGE_NAME", nullable = false, length = 50)
	private String cageName;

	protected CageHistory() {
	}

	public CageHistory(
			CageIdentity cageIdentity,
			DataCenterIdentity dataCenterIdentity,
			String cageName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			String appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.cageIdentity = cageIdentity;
		this.dataCenterIdentity = dataCenterIdentity;
		this.cageName = cageName;
	}

	public Long getCageHistoryId() {
		return cageHistoryId;
	}

	public CageIdentity getCageIdentity() {
		return cageIdentity;
	}

	public Long getCageId() {
		return cageIdentity.getCageId();
	}

	public DataCenterIdentity getDataCenterIdentity() {
		return dataCenterIdentity;
	}

	public Long getDataCenterId() {
		return dataCenterIdentity.getDataCenterId();
	}

	public String getCageName() {
		return cageName;
	}
}
