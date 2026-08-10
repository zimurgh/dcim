package com.dcim.workflow;

import java.time.Instant;

import com.dcim.organization.firm.FirmIdentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_CHANGE_SPEC")
public class ChangeSpec {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHANGE_SPEC_ID", nullable = false)
	private Long changeSpecId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "OWNER_FIRM_ID", nullable = false)
	private FirmIdentity ownerFirm;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 50)
	private ChangeSpecStatus status;

	@Column(name = "NAME", length = 100)
	private String name;

	@Column(name = "CREATED_AT", nullable = false)
	private Instant createdAt;

	@Column(name = "CREATED_BY", length = 50)
	private String createdBy;

	protected ChangeSpec() {
	}

	public ChangeSpec(FirmIdentity ownerFirm, String name, ChangeSpecStatus status, Instant createdAt, String createdBy) {
		this.ownerFirm = ownerFirm;
		this.name = name;
		this.status = status;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
	}

	public Long getChangeSpecId() {
		return changeSpecId;
	}

	public FirmIdentity getOwnerFirm() {
		return ownerFirm;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ChangeSpecStatus getStatus() {
		return status;
	}

	public void setStatus(ChangeSpecStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}
}
