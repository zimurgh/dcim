package com.dcim.connectivity.document;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
class DocumentController {

	private final DocumentService documents;

	DocumentController(DocumentService documents) {
		this.documents = documents;
	}

	@GetMapping
	List<DocumentDto> list(@RequestParam(required = false) Long crossConnectId) {
		if (crossConnectId != null) {
			return documents.listCurrentByCrossConnect(crossConnectId);
		}
		return documents.listCurrent();
	}

	@GetMapping("/{documentId}")
	DocumentDto get(@PathVariable Long documentId) {
		return documents.findCurrent(documentId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Document not found: " + documentId));
	}

	@GetMapping("/{documentId}/history")
	List<DocumentDto> history(@PathVariable Long documentId) {
		List<DocumentDto> rows = documents.history(documentId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
		}
		return rows;
	}
}
