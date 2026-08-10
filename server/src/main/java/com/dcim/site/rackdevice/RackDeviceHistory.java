package com.dcim.site.rackdevice;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_HISTORY")
public class RackDeviceHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_HISTORY_ID", nullable = false)
	private Long rackDeviceHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_ID", nullable = false)
	private RackDeviceIdentity rackDeviceIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_ID", nullable = false)
	private RackIdentity rackIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_TYPE_ID", nullable = false)
	private RackDeviceTypeIdentity rackDeviceTypeIdentity;

	@Column(name = "RACK_DEVICE_NAME", nullable = false, length = 50)
	private String rackDeviceName;

	protected RackDeviceHistory() {
	}

	public RackDeviceHistory(
			RackDeviceIdentity rackDeviceIdentity,
			RackIdentity rackIdentity,
			RackDeviceTypeIdentity rackDeviceTypeIdentity,
			String rackDeviceName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.rackDeviceIdentity = rackDeviceIdentity;
		this.rackIdentity = rackIdentity;
		this.rackDeviceTypeIdentity = rackDeviceTypeIdentity;
		this.rackDeviceName = rackDeviceName;
	}

	public Long getRackDeviceHistoryId() {
		return rackDeviceHistoryId;
	}

	public RackDeviceIdentity getRackDeviceIdentity() {
		return rackDeviceIdentity;
	}

	public Long getRackDeviceId() {
		return rackDeviceIdentity.getRackDeviceId();
	}

	public RackIdentity getRackIdentity() {
		return rackIdentity;
	}

	public Long getRackId() {
		return rackIdentity.getRackId();
	}

	public RackDeviceTypeIdentity getRackDeviceTypeIdentity() {
		return rackDeviceTypeIdentity;
	}

	public Long getRackDeviceTypeId() {
		return rackDeviceTypeIdentity.getRackDeviceTypeId();
	}

	public String getRackDeviceName() {
		return rackDeviceName;
	}
}
