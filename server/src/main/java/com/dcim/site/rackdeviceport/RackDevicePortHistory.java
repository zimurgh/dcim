package com.dcim.site.rackdeviceport;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.site.rackdevice.RackDeviceIdentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_RACK_DEVICE_PORT_HISTORY")
public class RackDevicePortHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_PORT_HISTORY_ID", nullable = false)
	private Long rackDevicePortHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_PORT_ID", nullable = false)
	private RackDevicePortIdentity rackDevicePortIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_ID", nullable = false)
	private RackDeviceIdentity rackDeviceIdentity;

	@Column(name = "RACK_DEVICE_PORT_NAME", nullable = false, length = 50)
	private String rackDevicePortName;

	protected RackDevicePortHistory() {
	}

	public RackDevicePortHistory(
			RackDevicePortIdentity rackDevicePortIdentity,
			RackDeviceIdentity rackDeviceIdentity,
			String rackDevicePortName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			String appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.rackDevicePortIdentity = rackDevicePortIdentity;
		this.rackDeviceIdentity = rackDeviceIdentity;
		this.rackDevicePortName = rackDevicePortName;
	}

	public Long getRackDevicePortHistoryId() {
		return rackDevicePortHistoryId;
	}

	public RackDevicePortIdentity getRackDevicePortIdentity() {
		return rackDevicePortIdentity;
	}

	public Long getRackDevicePortId() {
		return rackDevicePortIdentity.getRackDevicePortId();
	}

	public RackDeviceIdentity getRackDeviceIdentity() {
		return rackDeviceIdentity;
	}

	public Long getRackDeviceId() {
		return rackDeviceIdentity.getRackDeviceId();
	}

	public String getRackDevicePortName() {
		return rackDevicePortName;
	}
}
