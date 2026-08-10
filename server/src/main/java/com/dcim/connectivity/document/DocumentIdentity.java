package com.dcim.connectivity.document;

import jakarta.persistence.*;

@Entity
@Table(name = "T_DOCUMENT_IDENTITY")
public class DocumentIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DOCUMENT_ID", nullable = false)
	private Long documentId;

	public DocumentIdentity() {
	}

	public Long getDocumentId() {
		return documentId;
	}
}
