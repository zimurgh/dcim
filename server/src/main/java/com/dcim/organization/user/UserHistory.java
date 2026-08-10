package com.dcim.organization.user;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_USER_HISTORY")
public class UserHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "USER_HISTORY_ID", nullable = false)
	private Long userHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ID", nullable = false)
	private UserIdentity userIdentity;

	@Column(name = "USER_NAME", nullable = false, length = 50)
	private String userName;

	@Column(name = "IS_INITIATOR", nullable = false)
	private boolean isInitiator;

	protected UserHistory() {
	}

	public UserHistory(
			UserIdentity userIdentity,
			String userName,
			boolean isInitiator,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.userIdentity = userIdentity;
		this.userName = userName;
		this.isInitiator = isInitiator;
	}

	public Long getUserHistoryId() {
		return userHistoryId;
	}

	public UserIdentity getUserIdentity() {
		return userIdentity;
	}

	public Long getUserId() {
		return userIdentity.getUserId();
	}

	public String getUserName() {
		return userName;
	}

	public boolean isInitiator() {
		return isInitiator;
	}
}
