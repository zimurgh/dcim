package com.dcim.site.rackdevicetype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_TYPE_IDENTITY")
public class RackDeviceTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_TYPE_ID", nullable = false)
	private Long rackDeviceTypeId;

	public RackDeviceTypeIdentity() {
	}

	public Long getRackDeviceTypeId() {
		return rackDeviceTypeId;
	}
}
