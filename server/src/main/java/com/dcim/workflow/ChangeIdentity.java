package com.dcim.workflow;

import jakarta.persistence.*;

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
