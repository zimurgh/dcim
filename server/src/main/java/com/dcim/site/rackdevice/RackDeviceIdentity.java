package com.dcim.site.rackdevice;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_IDENTITY")
public class RackDeviceIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_ID", nullable = false)
	private Long rackDeviceId;

	public RackDeviceIdentity() {
	}

	public Long getRackDeviceId() {
		return rackDeviceId;
	}
}
