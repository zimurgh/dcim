package com.dcim.connectivity.chargetype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHARGE_TYPE_HISTORY")
public class ChargeTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHARGE_TYPE_HISTORY_ID", nullable = false)
	private Long chargeTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHARGE_TYPE_ID", nullable = false)
	private ChargeTypeIdentity chargeTypeIdentity;

	@Column(name = "CHARGE_TYPE_NAME", nullable = false, length = 100)
	private String chargeTypeName;

	protected ChargeTypeHistory() {
	}

	public ChargeTypeHistory(
			ChargeTypeIdentity chargeTypeIdentity,
			String chargeTypeName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.chargeTypeIdentity = chargeTypeIdentity;
		this.chargeTypeName = chargeTypeName;
	}

	public Long getChargeTypeHistoryId() {
		return chargeTypeHistoryId;
	}

	public ChargeTypeIdentity getChargeTypeIdentity() {
		return chargeTypeIdentity;
	}

	public Long getChargeTypeId() {
		return chargeTypeIdentity.getChargeTypeId();
	}

	public String getChargeTypeName() {
		return chargeTypeName;
	}
}
