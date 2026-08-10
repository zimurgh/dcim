package com.dcim.site.rackdevicetype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_RACK_DEVICE_TYPE_HISTORY")
public class RackDeviceTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RACK_DEVICE_TYPE_HISTORY_ID", nullable = false)
	private Long rackDeviceTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_TYPE_ID", nullable = false)
	private RackDeviceTypeIdentity rackDeviceTypeIdentity;

	@Column(name = "RACK_DEVICE_TYPE_NAME", nullable = false, length = 100)
	private String rackDeviceTypeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "RACK_DEVICE_TYPE_KIND", nullable = false, length = 50)
	private RackDeviceTypeKind rackDeviceTypeKind;

	protected RackDeviceTypeHistory() {
	}

	public RackDeviceTypeHistory(
			RackDeviceTypeIdentity rackDeviceTypeIdentity,
			String rackDeviceTypeName,
			RackDeviceTypeKind rackDeviceTypeKind,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.rackDeviceTypeIdentity = rackDeviceTypeIdentity;
		this.rackDeviceTypeName = rackDeviceTypeName;
		this.rackDeviceTypeKind = rackDeviceTypeKind;
	}

	public Long getRackDeviceTypeHistoryId() {
		return rackDeviceTypeHistoryId;
	}

	public RackDeviceTypeIdentity getRackDeviceTypeIdentity() {
		return rackDeviceTypeIdentity;
	}

	public Long getRackDeviceTypeId() {
		return rackDeviceTypeIdentity.getRackDeviceTypeId();
	}

	public String getRackDeviceTypeName() {
		return rackDeviceTypeName;
	}

	public RackDeviceTypeKind getRackDeviceTypeKind() {
		return rackDeviceTypeKind;
	}
}
