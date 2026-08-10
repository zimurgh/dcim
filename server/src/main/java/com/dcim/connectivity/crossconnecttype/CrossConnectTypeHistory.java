package com.dcim.connectivity.crossconnecttype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CROSS_CONNECT_TYPE_HISTORY")
public class CrossConnectTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CROSS_CONNECT_TYPE_HISTORY_ID", nullable = false)
	private Long crossConnectTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_TYPE_ID", nullable = false)
	private CrossConnectTypeIdentity crossConnectTypeIdentity;

	@Column(name = "CROSS_CONNECT_TYPE_NAME", nullable = false, length = 100)
	private String crossConnectTypeName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CHARGE_TYPE_ID")
	private ChargeTypeIdentity chargeTypeIdentity;

	protected CrossConnectTypeHistory() {
	}

	public CrossConnectTypeHistory(
			CrossConnectTypeIdentity crossConnectTypeIdentity,
			String crossConnectTypeName,
			ChargeTypeIdentity chargeTypeIdentity,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.crossConnectTypeIdentity = crossConnectTypeIdentity;
		this.crossConnectTypeName = crossConnectTypeName;
		this.chargeTypeIdentity = chargeTypeIdentity;
	}

	public Long getCrossConnectTypeHistoryId() {
		return crossConnectTypeHistoryId;
	}

	public CrossConnectTypeIdentity getCrossConnectTypeIdentity() {
		return crossConnectTypeIdentity;
	}

	public Long getCrossConnectTypeId() {
		return crossConnectTypeIdentity.getCrossConnectTypeId();
	}

	public String getCrossConnectTypeName() {
		return crossConnectTypeName;
	}

	public ChargeTypeIdentity getChargeTypeIdentity() {
		return chargeTypeIdentity;
	}

	public Long getChargeTypeId() {
		return chargeTypeIdentity == null ? null : chargeTypeIdentity.getChargeTypeId();
	}
}
