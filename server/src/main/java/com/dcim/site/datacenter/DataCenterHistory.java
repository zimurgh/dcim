package com.dcim.site.datacenter;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_DATA_CENTER_HISTORY")
public class DataCenterHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DATA_CENTER_HISTORY_ID", nullable = false)
	private Long dataCenterHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "DATA_CENTER_ID", nullable = false)
	private DataCenterIdentity dataCenterIdentity;

	@Column(name = "DATA_CENTER_NAME", nullable = false, length = 50)
	private String dataCenterName;

	protected DataCenterHistory() {
	}

	public DataCenterHistory(
			DataCenterIdentity dataCenterIdentity,
			String dataCenterName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.dataCenterIdentity = dataCenterIdentity;
		this.dataCenterName = dataCenterName;
	}

	public Long getDataCenterHistoryId() {
		return dataCenterHistoryId;
	}

	public DataCenterIdentity getDataCenterIdentity() {
		return dataCenterIdentity;
	}

	public Long getDataCenterId() {
		return dataCenterIdentity.getDataCenterId();
	}

	public String getDataCenterName() {
		return dataCenterName;
	}
}
