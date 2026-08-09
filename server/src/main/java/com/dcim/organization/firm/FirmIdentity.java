package com.dcim.organization.firm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stable firm identity across history revisions.
 */
@Entity
@Table(name = "T_FIRM_IDENTITY")
public class FirmIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "FIRM_ID", nullable = false)
	private Long firmId;

	protected FirmIdentity() {
	}

	public Long getFirmId() {
		return firmId;
	}
}
