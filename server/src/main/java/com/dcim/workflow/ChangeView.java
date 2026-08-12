package com.dcim.workflow;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity
@Immutable
@Table(name = "V_CHANGE")
public class ChangeView {

	@Id
	@Column(name = "CHANGE_ID", nullable = false)
	private Long changeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "STAGE", nullable = false, length = 50)
	private ChangeStage stage;

	@Column(name = "STATUS_LABEL", nullable = false, length = 50)
	private String statusLabel;

	@Column(name = "BODY", nullable = false, columnDefinition = "CLOB")
	private String body;

	@Column(name = "ASSET_TYPE_ID")
	private Long assetTypeId;

	@Column(name = "ASSET_TYPE", length = 50)
	private String assetType;

	@Enumerated(EnumType.STRING)
	@Column(name = "ACTION", length = 50)
	private ChangeAction action;

	@Column(name = "ASSET_IDENTITY_ID")
	private Long assetIdentityId;

	@Column(name = "BASE_HISTORY_ID")
	private Long baseHistoryId;

	@Column(name = "CHANGE_PAYLOAD_ID", nullable = false)
	private Long changePayloadId;

	@Column(name = "EVENT_AT", nullable = false)
	private Instant eventAt;

	@Column(name = "ACTOR", length = 50)
	private String actor;

	@Column(name = "APPLIED_BY")
	private Long appliedBy;

	@Column(name = "APPLIED_BY_NAME", length = 50)
	private String appliedByName;

	@Column(name = "CHANGE_SPEC_ID")
	private Long changeSpecId;

	protected ChangeView() {
	}

	public Long getChangeId() {
		return changeId;
	}

	public ChangeStage getStage() {
		return stage;
	}

	public String getStatusLabel() {
		return statusLabel;
	}

	public String getBody() {
		return body;
	}

	public Long getAssetTypeId() {
		return assetTypeId;
	}

	public String getAssetType() {
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

	public Long getChangePayloadId() {
		return changePayloadId;
	}

	public Instant getEventAt() {
		return eventAt;
	}

	public String getActor() {
		return actor;
	}

	public Long getAppliedBy() {
		return appliedBy;
	}

	public String getAppliedByName() {
		return appliedByName;
	}

	public Long getChangeSpecId() {
		return changeSpecId;
	}
}
