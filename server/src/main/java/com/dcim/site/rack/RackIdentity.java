package com.dcim.site.rack;

import jakarta.persistence.*;

/**
 * Stable rack identity across history revisions.
 */
@Entity
@Table(name = "T_RACK_IDENTITY")
public class RackIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_ID", nullable = false)
	private Long rackId;

	public RackIdentity() {
	}

	public Long getRackId() {
		return rackId;
	}
}
