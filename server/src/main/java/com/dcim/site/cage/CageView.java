package com.dcim.site.cage;

import com.dcim.asset.AuditHistory;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

/**
 * Read model for cage history with the parent data center's current name
 * instead of a {@link com.dcim.site.datacenter.DataCenterIdentity} association.
 */
@Entity
@Immutable
@Table(name = "V_CAGE")
public class CageView extends AuditHistory {

	@Id
	@Column(name = "CAGE_HISTORY_ID", nullable = false)
	private Long cageHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CAGE_ID", nullable = false)
	private CageIdentity cageIdentity;

	@Column(name = "DATA_CENTER_ID", nullable = false)
	private Long dataCenterId;

	@Column(name = "DATA_CENTER_NAME", nullable = false, length = 50)
	private String dataCenterName;

	@Column(name = "CAGE_NAME", nullable = false, length = 50)
	private String cageName;

	protected CageView() {
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

	public Long getDataCenterId() {
		return dataCenterId;
	}

	public String getDataCenterName() {
		return dataCenterName;
	}

	public String getCageName() {
		return cageName;
	}
}
