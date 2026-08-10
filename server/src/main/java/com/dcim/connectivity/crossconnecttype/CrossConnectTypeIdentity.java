package com.dcim.connectivity.crossconnecttype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CROSS_CONNECT_TYPE_IDENTITY")
public class CrossConnectTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CROSS_CONNECT_TYPE_ID", nullable = false)
	private Long crossConnectTypeId;

	public CrossConnectTypeIdentity() {
	}

	public Long getCrossConnectTypeId() {
		return crossConnectTypeId;
	}
}
