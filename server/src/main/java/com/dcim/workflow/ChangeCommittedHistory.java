package com.dcim.workflow;

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
@Table(name = "T_CHANGE_COMMITTED_HISTORY")
public class ChangeCommittedHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHANGE_COMMITTED_HISTORY_ID", nullable = false)
	private Long changeCommittedHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_ID", nullable = false)
	private ChangeCommitted committed;

	@Enumerated(EnumType.STRING)
	@Column(name = "ASSET_TYPE", nullable = false, length = 50)
	private AssetType assetType;

	@Column(name = "HISTORY_ID", nullable = false)
	private Long historyId;

	@Enumerated(EnumType.STRING)
	@Column(name = "ROLE", nullable = false, length = 50)
	private HistoryLinkRole role;

	protected ChangeCommittedHistory() {
	}

	public ChangeCommittedHistory(
			ChangeCommitted committed,
			AssetType assetType,
			Long historyId,
			HistoryLinkRole role) {
		this.committed = committed;
		this.assetType = assetType;
		this.historyId = historyId;
		this.role = role;
	}

	public Long getChangeCommittedHistoryId() {
		return changeCommittedHistoryId;
	}

	public ChangeCommitted getCommitted() {
		return committed;
	}

	public AssetType getAssetType() {
		return assetType;
	}

	public Long getHistoryId() {
		return historyId;
	}

	public HistoryLinkRole getRole() {
		return role;
	}
}
