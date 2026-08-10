package com.dcim.connectivity.crossconnect;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeIdentity;
import com.dcim.connectivity.latency.LatencyIdentity;
import com.dcim.connectivity.speed.SpeedIdentity;
import com.dcim.organization.firm.FirmIdentity;
import com.dcim.organization.marketsegment.MarketSegmentIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CROSS_CONNECT_HISTORY")
public class CrossConnectHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CROSS_CONNECT_HISTORY_ID", nullable = false)
	private Long crossConnectHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID", nullable = false)
	private CrossConnectIdentity crossConnectIdentity;

	@Column(name = "CROSS_CONNECT_NAME", nullable = false, length = 100)
	private String crossConnectName;

	@Column(name = "CIRCUIT_ID", nullable = false, length = 100)
	private String circuitId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_TYPE_ID", nullable = false)
	private CrossConnectTypeIdentity crossConnectTypeIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "LATENCY_ID", nullable = false)
	private LatencyIdentity latencyIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "SPEED_ID", nullable = false)
	private SpeedIdentity speedIdentity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MARKET_SEGMENT_ID")
	private MarketSegmentIdentity marketSegmentIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "OWNER_FIRM_ID", nullable = false)
	private FirmIdentity ownerFirmIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "BILLING_FIRM_ID", nullable = false)
	private FirmIdentity billingFirmIdentity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROVIDER_FIRM_ID")
	private FirmIdentity providerFirmIdentity;

	protected CrossConnectHistory() {
	}

	public CrossConnectHistory(
			CrossConnectIdentity crossConnectIdentity,
			String crossConnectName,
			String circuitId,
			CrossConnectTypeIdentity crossConnectTypeIdentity,
			LatencyIdentity latencyIdentity,
			SpeedIdentity speedIdentity,
			MarketSegmentIdentity marketSegmentIdentity,
			FirmIdentity ownerFirmIdentity,
			FirmIdentity billingFirmIdentity,
			FirmIdentity providerFirmIdentity,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.crossConnectIdentity = crossConnectIdentity;
		this.crossConnectName = crossConnectName;
		this.circuitId = circuitId;
		this.crossConnectTypeIdentity = crossConnectTypeIdentity;
		this.latencyIdentity = latencyIdentity;
		this.speedIdentity = speedIdentity;
		this.marketSegmentIdentity = marketSegmentIdentity;
		this.ownerFirmIdentity = ownerFirmIdentity;
		this.billingFirmIdentity = billingFirmIdentity;
		this.providerFirmIdentity = providerFirmIdentity;
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

	public CrossConnectTypeIdentity getCrossConnectTypeIdentity() {
		return crossConnectTypeIdentity;
	}

	public Long getCrossConnectTypeId() {
		return crossConnectTypeIdentity.getCrossConnectTypeId();
	}

	public LatencyIdentity getLatencyIdentity() {
		return latencyIdentity;
	}

	public Long getLatencyId() {
		return latencyIdentity.getLatencyId();
	}

	public SpeedIdentity getSpeedIdentity() {
		return speedIdentity;
	}

	public Long getSpeedId() {
		return speedIdentity.getSpeedId();
	}

	public MarketSegmentIdentity getMarketSegmentIdentity() {
		return marketSegmentIdentity;
	}

	public Long getMarketSegmentId() {
		return marketSegmentIdentity == null ? null : marketSegmentIdentity.getMarketSegmentId();
	}

	public FirmIdentity getOwnerFirmIdentity() {
		return ownerFirmIdentity;
	}

	public Long getOwnerFirmId() {
		return ownerFirmIdentity.getFirmId();
	}

	public FirmIdentity getBillingFirmIdentity() {
		return billingFirmIdentity;
	}

	public Long getBillingFirmId() {
		return billingFirmIdentity.getFirmId();
	}

	public FirmIdentity getProviderFirmIdentity() {
		return providerFirmIdentity;
	}

	public Long getProviderFirmId() {
		return providerFirmIdentity == null ? null : providerFirmIdentity.getFirmId();
	}
}
