package com.dcim.workflow;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHANGE_COMMITTED")
public class ChangeCommitted {

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

	@Column(name = "APPLIED_AT", nullable = false)
	private Instant appliedAt;

	@Column(name = "APPLIED_BY", length = 50)
	private String appliedBy;

	protected ChangeCommitted() {
	}

	public ChangeCommitted(
			ChangeIdentity changeIdentity,
			ChangePayload payload,
			AssetType assetType,
			ChangeAction action,
			Instant appliedAt,
			String appliedBy) {
		this.changeIdentity = changeIdentity;
		this.changeId = changeIdentity.getChangeId();
		this.payload = payload;
		this.assetType = assetType;
		this.action = action;
		this.appliedAt = appliedAt;
		this.appliedBy = appliedBy;
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

	public AssetType getAssetType() {
		return assetType;
	}

	public ChangeAction getAction() {
		return action;
	}

	public Instant getAppliedAt() {
		return appliedAt;
	}

	public String getAppliedBy() {
		return appliedBy;
	}
}
