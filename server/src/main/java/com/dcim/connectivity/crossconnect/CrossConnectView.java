package com.dcim.connectivity.crossconnect;

import com.dcim.asset.AuditHistory;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity
@Immutable
@Table(name = "V_CROSS_CONNECT")
public class CrossConnectView extends AuditHistory {

	@Id
	@Column(name = "CROSS_CONNECT_HISTORY_ID", nullable = false)
	private Long crossConnectHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID", nullable = false)
	private CrossConnectIdentity crossConnectIdentity;

	@Column(name = "CROSS_CONNECT_NAME", nullable = false, length = 100)
	private String crossConnectName;

	@Column(name = "CIRCUIT_ID", nullable = false, length = 100)
	private String circuitId;

	@Column(name = "CROSS_CONNECT_TYPE_ID", nullable = false)
	private Long crossConnectTypeId;

	@Column(name = "CROSS_CONNECT_TYPE_NAME", nullable = false, length = 100)
	private String crossConnectTypeName;

	@Column(name = "LATENCY_ID", nullable = false)
	private Long latencyId;

	@Column(name = "LATENCY_NAME", nullable = false, length = 100)
	private String latencyName;

	@Column(name = "SPEED_ID", nullable = false)
	private Long speedId;

	@Column(name = "SPEED_NAME", nullable = false, length = 100)
	private String speedName;

	@Column(name = "MARKET_SEGMENT_ID")
	private Long marketSegmentId;

	@Column(name = "MARKET_SEGMENT_NAME", length = 100)
	private String marketSegmentName;

	@Column(name = "OWNER_FIRM_ID", nullable = false)
	private Long ownerFirmId;

	@Column(name = "OWNER_FIRM_NAME", nullable = false, length = 50)
	private String ownerFirmName;

	@Column(name = "BILLING_FIRM_ID", nullable = false)
	private Long billingFirmId;

	@Column(name = "BILLING_FIRM_NAME", nullable = false, length = 50)
	private String billingFirmName;

	@Column(name = "PROVIDER_FIRM_ID")
	private Long providerFirmId;

	@Column(name = "PROVIDER_FIRM_NAME", length = 50)
	private String providerFirmName;

	protected CrossConnectView() {
	}

	public Long getCrossConnectHistoryId() {
		return crossConnectHistoryId;
	}

	public CrossConnectIdentity getCrossConnectIdentity() {
		return crossConnectIdentity;
	}

	public Long getCrossConnectId() {
		return crossConnectIdentity.getCrossConnectId();
	}

	public String getCrossConnectName() {
		return crossConnectName;
	}

	public String getCircuitId() {
		return circuitId;
	}

	public Long getCrossConnectTypeId() {
		return crossConnectTypeId;
	}

	public String getCrossConnectTypeName() {
		return crossConnectTypeName;
	}

	public Long getLatencyId() {
		return latencyId;
	}

	public String getLatencyName() {
		return latencyName;
	}

	public Long getSpeedId() {
		return speedId;
	}

	public String getSpeedName() {
		return speedName;
	}

	public Long getMarketSegmentId() {
		return marketSegmentId;
	}

	public String getMarketSegmentName() {
		return marketSegmentName;
	}

	public Long getOwnerFirmId() {
		return ownerFirmId;
	}

	public String getOwnerFirmName() {
		return ownerFirmName;
	}

	public Long getBillingFirmId() {
		return billingFirmId;
	}

	public String getBillingFirmName() {
		return billingFirmName;
	}

	public Long getProviderFirmId() {
		return providerFirmId;
	}

	public String getProviderFirmName() {
		return providerFirmName;
	}
}
