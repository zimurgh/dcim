package com.dcim.site.cage;

import jakarta.persistence.*;

/**
 * Stable cage identity across history revisions.
 */
@Entity
@Table(name = "T_CAGE_IDENTITY")
public class CageIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CAGE_ID", nullable = false)
	private Long cageId;

	public CageIdentity() {
	}

	public Long getCageId() {
		return cageId;
	}
}
