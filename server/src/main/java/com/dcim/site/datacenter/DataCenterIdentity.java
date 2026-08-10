package com.dcim.site.datacenter;

import jakarta.persistence.*;

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
