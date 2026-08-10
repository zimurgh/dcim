package com.dcim.site.rackdeviceporttype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_PORT_TYPE_IDENTITY")
public class RackDevicePortTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_PORT_TYPE_ID", nullable = false)
	private Long rackDevicePortTypeId;

	public RackDevicePortTypeIdentity() {
	}

	public Long getRackDevicePortTypeId() {
		return rackDevicePortTypeId;
	}
}
