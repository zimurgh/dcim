package com.dcim.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_CHANGE_IDENTITY")
public class ChangeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHANGE_ID", nullable = false)
	private Long changeId;

	public ChangeIdentity() {
	}

	public Long getChangeId() {
		return changeId;
	}
}
