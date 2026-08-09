package com.dcim.site.rackdeviceport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stable rack device port identity across history revisions.
 */
@Entity
@Table(name = "T_RACK_DEVICE_PORT_IDENTITY")
public class RackDevicePortIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_PORT_ID", nullable = false)
	private Long rackDevicePortId;

	public RackDevicePortIdentity() {
	}

	public Long getRackDevicePortId() {
		return rackDevicePortId;
	}
}
