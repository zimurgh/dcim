package com.dcim.connectivity.document;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record DocumentDto(
		Long documentId,
		Long documentHistoryId,
		Long crossConnectId,
		String documentName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static DocumentDto from(DocumentHistory history) {
		return new DocumentDto(
				history.getDocumentId(),
				history.getDocumentHistoryId(),
				history.getCrossConnectId(),
				history.getDocumentName(),
				AuditSlice.from(history));
	}
}
