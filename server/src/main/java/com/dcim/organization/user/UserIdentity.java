package com.dcim.organization.user;

import jakarta.persistence.*;

/**
 * Stable user identity across history revisions.
 */
@Entity
@Table(name = "T_USER_IDENTITY")
public class UserIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "USER_ID", nullable = false)
	private Long userId;

	public UserIdentity() {
	}

	public Long getUserId() {
		return userId;
	}
}
