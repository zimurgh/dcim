package com.dcim.site.datacenter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_DATA_CENTER_IDENTITY")
public class DataCenterIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DATA_CENTER_ID", nullable = false)
	private Long dataCenterId;

	public DataCenterIdentity() {
	}

	public Long getDataCenterId() {
		return dataCenterId;
	}
}
