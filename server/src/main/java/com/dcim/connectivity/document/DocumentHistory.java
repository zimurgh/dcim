package com.dcim.connectivity.document;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;

import jakarta.persistence.*;

@Entity
@Table(name = "T_DOCUMENT_HISTORY")
public class DocumentHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DOCUMENT_HISTORY_ID", nullable = false)
	private Long documentHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "DOCUMENT_ID", nullable = false)
	private DocumentIdentity documentIdentity;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CROSS_CONNECT_ID", nullable = false)
	private CrossConnectIdentity crossConnectIdentity;

	@Column(name = "DOCUMENT_NAME", nullable = false, length = 200)
	private String documentName;

	protected DocumentHistory() {
	}

	public DocumentHistory(
			DocumentIdentity documentIdentity,
			CrossConnectIdentity crossConnectIdentity,
			String documentName,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.documentIdentity = documentIdentity;
		this.crossConnectIdentity = crossConnectIdentity;
		this.documentName = documentName;
	}

	public Long getDocumentHistoryId() {
		return documentHistoryId;
	}

	public DocumentIdentity getDocumentIdentity() {
		return documentIdentity;
	}

	public Long getDocumentId() {
		return documentIdentity.getDocumentId();
	}

	public CrossConnectIdentity getCrossConnectIdentity() {
		return crossConnectIdentity;
	}

	public Long getCrossConnectId() {
		return crossConnectIdentity.getCrossConnectId();
	}

	public String getDocumentName() {
		return documentName;
	}
}
