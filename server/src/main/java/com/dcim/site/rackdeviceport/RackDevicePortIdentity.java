package com.dcim.site.rackdeviceport;

import jakarta.persistence.*;

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
