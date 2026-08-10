package com.dcim.site.cage;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Shallow cage DTO for API responses (current history row).
 */
public record CageDto(
		Long cageId,
		Long cageHistoryId,
		Long dataCenterId,
		String cageName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static CageDto from(CageHistory history) {
		return new CageDto(
				history.getCageId(),
				history.getCageHistoryId(),
				history.getDataCenterId(),
				history.getCageName(),
				AuditSlice.from(history));
	}
}
