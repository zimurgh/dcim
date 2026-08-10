package com.dcim.connectivity.document;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentDto(
		Long documentId,
		Long documentHistoryId,
		Long crossConnectId,
		String documentName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static DocumentDto from(DocumentHistory history) {
		return new DocumentDto(
				history.getDocumentId(),
				history.getDocumentHistoryId(),
				history.getCrossConnectId(),
				history.getDocumentName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
