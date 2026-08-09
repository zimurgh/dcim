package com.dcim.site.rack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
