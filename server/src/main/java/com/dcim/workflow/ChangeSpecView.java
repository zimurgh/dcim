package com.dcim.workflow;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity
@Immutable
@Table(name = "V_CHANGE_SPEC")
public class ChangeSpecView {

	@Id
	@Column(name = "CHANGE_SPEC_ID", nullable = false)
	private Long changeSpecId;

	@Column(name = "OWNER_FIRM_ID", nullable = false)
	private Long ownerFirmId;

	@Column(name = "OWNER_FIRM_NAME", nullable = false, length = 50)
	private String ownerFirmName;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 50)
	private ChangeSpecStatus status;

	@Column(name = "NAME", length = 100)
	private String name;

	@Column(name = "CREATED_AT", nullable = false)
	private Instant createdAt;

	@Column(name = "CREATED_BY", length = 50)
	private String createdBy;

	protected ChangeSpecView() {
	}

	public Long getChangeSpecId() {
		return changeSpecId;
	}

	public Long getOwnerFirmId() {
		return ownerFirmId;
	}

	public String getOwnerFirmName() {
		return ownerFirmName;
	}

	public ChangeSpecStatus getStatus() {
		return status;
	}

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}
}
