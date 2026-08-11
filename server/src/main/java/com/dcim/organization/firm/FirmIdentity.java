package com.dcim.organization.firm;

import jakarta.persistence.*;

@Entity
@Table(name = "T_FIRM_IDENTITY")
public class FirmIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "FIRM_ID", nullable = false)
	private Long firmId;

	public FirmIdentity() {
	}

	public Long getFirmId() {
		return firmId;
	}
}
