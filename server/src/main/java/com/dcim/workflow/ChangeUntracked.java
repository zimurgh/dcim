package com.dcim.workflow;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHANGE_UNTRACKED")
public class ChangeUntracked {

	@Id
	@Column(name = "CHANGE_ID")
	private Long changeId;

	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_ID", insertable = false, updatable = false)
	private ChangeIdentity changeIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_PAYLOAD_ID", nullable = false)
	private ChangePayload payload;

	@Column(name = "CREATED_AT", nullable = false)
	private Instant createdAt;

	@Column(name = "CREATED_BY", length = 50)
	private String createdBy;

	protected ChangeUntracked() {
	}

	public ChangeUntracked(ChangeIdentity changeIdentity, ChangePayload payload, Instant createdAt, String createdBy) {
		this.changeIdentity = changeIdentity;
		this.changeId = changeIdentity.getChangeId();
		this.payload = payload;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}
}
