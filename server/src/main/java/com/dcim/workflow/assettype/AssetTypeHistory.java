package com.dcim.workflow.assettype;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_ASSET_TYPE_HISTORY")
public class AssetTypeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ASSET_TYPE_HISTORY_ID", nullable = false)
	private Long assetTypeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_TYPE_ID", nullable = false)
	private AssetTypeIdentity assetTypeIdentity;

	@Column(name = "ASSET_TYPE_CODE", nullable = false, length = 50)
	private String assetTypeCode;

	@Column(name = "ASSET_TYPE_NAME", nullable = false, length = 100)
	private String assetTypeName;

	@Column(name = "APPLY_RANK", nullable = false)
	private int applyRank;

	protected AssetTypeHistory() {
	}

	public AssetTypeHistory(
			AssetTypeIdentity assetTypeIdentity,
			String assetTypeCode,
			String assetTypeName,
			int applyRank,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.assetTypeIdentity = assetTypeIdentity;
		this.assetTypeCode = assetTypeCode;
		this.assetTypeName = assetTypeName;
		this.applyRank = applyRank;
	}

	public Long getAssetTypeHistoryId() {
		return assetTypeHistoryId;
	}

	public AssetTypeIdentity getAssetTypeIdentity() {
		return assetTypeIdentity;
	}

	public Long getAssetTypeId() {
		return assetTypeIdentity.getAssetTypeId();
	}

	public String getAssetTypeCode() {
		return assetTypeCode;
	}

	public String getAssetTypeName() {
		return assetTypeName;
	}

	public int getApplyRank() {
		return applyRank;
	}
}
