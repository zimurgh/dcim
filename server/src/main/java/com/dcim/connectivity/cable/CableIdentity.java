package com.dcim.connectivity.cable;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CABLE_IDENTITY")
public class CableIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CABLE_ID", nullable = false)
	private Long cableId;

	public CableIdentity() {
	}

	public Long getCableId() {
		return cableId;
	}
}
