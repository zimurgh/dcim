package com.dcim.site.datacenter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stable data center identity across history revisions.
 */
@Entity
@Table(name = "T_DATA_CENTER_IDENTITY")
public class DataCenterIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DATA_CENTER_ID", nullable = false)
	private Long dataCenterId;

	protected DataCenterIdentity() {
	}

	public Long getDataCenterId() {
		return dataCenterId;
	}
}
