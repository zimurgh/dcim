package com.dcim.connectivity.crossconnect;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CROSS_CONNECT_IDENTITY")
public class CrossConnectIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CROSS_CONNECT_ID", nullable = false)
	private Long crossConnectId;

	public CrossConnectIdentity() {
	}

	public Long getCrossConnectId() {
		return crossConnectId;
	}
}
