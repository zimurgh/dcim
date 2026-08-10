package com.dcim.connectivity.document;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

	private final DocumentHistoryRepository documents;

	DocumentService(DocumentHistoryRepository documents) {
		this.documents = documents;
	}

	@Transactional(readOnly = true)
	public List<DocumentDto> listCurrent() {
		return documents.findCurrent().stream().map(DocumentDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<DocumentDto> listCurrentByCrossConnect(Long crossConnectId) {
		return documents.findCurrentByCrossConnectId(crossConnectId).stream().map(DocumentDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<DocumentDto> findCurrent(Long documentId) {
		return documents.findCurrentByDocumentId(documentId).map(DocumentDto::from);
	}

	@Transactional(readOnly = true)
	public List<DocumentDto> history(Long documentId) {
		return documents.findByDocumentIdentity_DocumentIdOrderByDocumentHistoryIdAsc(documentId).stream()
				.map(DocumentDto::from)
				.toList();
	}
}
