package com.dcim.site.rackdeviceporttype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_PORT_TYPE_HISTORY")
public class RackDevicePortTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_PORT_TYPE_HISTORY_ID", nullable = false)
	private Long rackDevicePortTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_PORT_TYPE_ID", nullable = false)
	private RackDevicePortTypeIdentity rackDevicePortTypeIdentity;

	@Column(name = "RACK_DEVICE_PORT_TYPE_NAME", nullable = false, length = 100)
	private String rackDevicePortTypeName;

	protected RackDevicePortTypeHistory() {
	}

	public RackDevicePortTypeHistory(
			RackDevicePortTypeIdentity rackDevicePortTypeIdentity,
			String rackDevicePortTypeName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.rackDevicePortTypeIdentity = rackDevicePortTypeIdentity;
		this.rackDevicePortTypeName = rackDevicePortTypeName;
	}

	public Long getRackDevicePortTypeHistoryId() {
		return rackDevicePortTypeHistoryId;
	}

	public RackDevicePortTypeIdentity getRackDevicePortTypeIdentity() {
		return rackDevicePortTypeIdentity;
	}

	public Long getRackDevicePortTypeId() {
		return rackDevicePortTypeIdentity.getRackDevicePortTypeId();
	}

	public String getRackDevicePortTypeName() {
		return rackDevicePortTypeName;
	}
}
