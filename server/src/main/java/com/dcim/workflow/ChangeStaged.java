package com.dcim.workflow;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_CHANGE_STAGED")
public class ChangeStaged {

	@Id
	@Column(name = "CHANGE_ID")
	private Long changeId;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_ID", insertable = false, updatable = false)
	private ChangeIdentity changeIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_PAYLOAD_ID", nullable = false)
	private ChangePayload payload;

	@Enumerated(EnumType.STRING)
	@Column(name = "ASSET_TYPE", nullable = false, length = 50)
	private AssetType assetType;

	@Enumerated(EnumType.STRING)
	@Column(name = "ACTION", nullable = false, length = 50)
	private ChangeAction action;

	@Column(name = "ASSET_IDENTITY_ID")
	private Long assetIdentityId;

	@Column(name = "BASE_HISTORY_ID")
	private Long baseHistoryId;

	@Column(name = "STAGED_AT", nullable = false)
	private Instant stagedAt;

	@Column(name = "STAGED_BY", length = 50)
	private String stagedBy;

	protected ChangeStaged() {
	}

	public ChangeStaged(
			ChangeIdentity changeIdentity,
			ChangePayload payload,
			AssetType assetType,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			Instant stagedAt,
			String stagedBy) {
		this.changeIdentity = changeIdentity;
		this.changeId = changeIdentity.getChangeId();
		this.payload = payload;
		this.assetType = assetType;
		this.action = action;
		this.assetIdentityId = assetIdentityId;
		this.baseHistoryId = baseHistoryId;
		this.stagedAt = stagedAt;
		this.stagedBy = stagedBy;
	}

	public Long getChangeId() {
		return changeId;
	}

	public ChangeIdentity getChangeIdentity() {
		return changeIdentity;
	}

	public ChangePayload getPayload() {
		return payload;
	}

	public void setPayload(ChangePayload payload) {
		this.payload = payload;
	}

	public AssetType getAssetType() {
		return assetType;
	}

	public ChangeAction getAction() {
		return action;
	}

	public Long getAssetIdentityId() {
		return assetIdentityId;
	}

	public Long getBaseHistoryId() {
		return baseHistoryId;
	}

	public Instant getStagedAt() {
		return stagedAt;
	}

	public String getStagedBy() {
		return stagedBy;
	}
}
