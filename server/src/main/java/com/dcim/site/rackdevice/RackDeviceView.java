package com.dcim.site.rackdevice;

import com.dcim.asset.AuditHistory;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity
@Immutable
@Table(name = "V_RACK_DEVICE")
public class RackDeviceView extends AuditHistory {

	@Id
	@Column(name = "RACK_DEVICE_HISTORY_ID", nullable = false)
	private Long rackDeviceHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_DEVICE_ID", nullable = false)
	private RackDeviceIdentity rackDeviceIdentity;

	@Column(name = "RACK_ID", nullable = false)
	private Long rackId;

	@Column(name = "RACK_NAME", nullable = false, length = 50)
	private String rackName;

	@Column(name = "CAGE_ID", nullable = false)
	private Long cageId;

	@Column(name = "CAGE_NAME", nullable = false, length = 50)
	private String cageName;

	@Column(name = "DATA_CENTER_ID", nullable = false)
	private Long dataCenterId;

	@Column(name = "DATA_CENTER_NAME", nullable = false, length = 50)
	private String dataCenterName;

	@Column(name = "RACK_DEVICE_NAME", nullable = false, length = 50)
	private String rackDeviceName;

	protected RackDeviceView() {
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

	public Long getRackId() {
		return rackId;
	}

	public String getRackName() {
		return rackName;
	}

	public Long getCageId() {
		return cageId;
	}

	public String getCageName() {
		return cageName;
	}

	public Long getDataCenterId() {
		return dataCenterId;
	}

	public String getDataCenterName() {
		return dataCenterName;
	}

	public String getRackDeviceName() {
		return rackDeviceName;
	}
}
