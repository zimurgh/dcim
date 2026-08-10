package com.dcim.connectivity.cable;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.site.rackdeviceport.RackDevicePortIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CABLE_HISTORY")
public class CableHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CABLE_HISTORY_ID", nullable = false)
	private Long cableHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CABLE_ID", nullable = false)
	private CableIdentity cableIdentity;

	@Column(name = "CABLE_NAME", nullable = false, length = 100)
	private String cableName;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "PORT_A_ID", nullable = false)
	private RackDevicePortIdentity portAIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "PORT_B_ID", nullable = false)
	private RackDevicePortIdentity portBIdentity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID")
	private CrossConnectIdentity crossConnectIdentity;

	protected CableHistory() {
	}

	public CableHistory(
			CableIdentity cableIdentity,
			String cableName,
			RackDevicePortIdentity portAIdentity,
			RackDevicePortIdentity portBIdentity,
			CrossConnectIdentity crossConnectIdentity,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.cableIdentity = cableIdentity;
		this.cableName = cableName;
		this.portAIdentity = portAIdentity;
		this.portBIdentity = portBIdentity;
		this.crossConnectIdentity = crossConnectIdentity;
	}

	public Long getCableHistoryId() {
		return cableHistoryId;
	}

	public CableIdentity getCableIdentity() {
		return cableIdentity;
	}

	public Long getCableId() {
		return cableIdentity.getCableId();
	}

	public String getCableName() {
		return cableName;
	}

	public RackDevicePortIdentity getPortAIdentity() {
		return portAIdentity;
	}

	public Long getPortAId() {
		return portAIdentity.getRackDevicePortId();
	}

	public RackDevicePortIdentity getPortBIdentity() {
		return portBIdentity;
	}

	public Long getPortBId() {
		return portBIdentity.getRackDevicePortId();
	}

	public CrossConnectIdentity getCrossConnectIdentity() {
		return crossConnectIdentity;
	}

	public Long getCrossConnectId() {
		return crossConnectIdentity == null ? null : crossConnectIdentity.getCrossConnectId();
	}
}
