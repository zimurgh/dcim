package com.dcim.connectivity.chargetype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHARGE_TYPE_IDENTITY")
public class ChargeTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHARGE_TYPE_ID", nullable = false)
	private Long chargeTypeId;

	public ChargeTypeIdentity() {
	}

	public Long getChargeTypeId() {
		return chargeTypeId;
	}
}
