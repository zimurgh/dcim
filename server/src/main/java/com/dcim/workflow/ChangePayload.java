package com.dcim.workflow;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHANGE_PAYLOAD")
public class ChangePayload {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHANGE_PAYLOAD_ID", nullable = false)
	private Long changePayloadId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_ID", nullable = false)
	private ChangeIdentity changeIdentity;

	@Column(name = "BODY", nullable = false, columnDefinition = "CLOB")
	private String body;

	@Column(name = "CREATED_AT", nullable = false)
	private Instant createdAt;

	protected ChangePayload() {
	}

	public ChangePayload(ChangeIdentity changeIdentity, String body, Instant createdAt) {
		this.changeIdentity = changeIdentity;
		this.body = body;
		this.createdAt = createdAt;
	}

	public Long getChangePayloadId() {
		return changePayloadId;
	}

	public ChangeIdentity getChangeIdentity() {
		return changeIdentity;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
