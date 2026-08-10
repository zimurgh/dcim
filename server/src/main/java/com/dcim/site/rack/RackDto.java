package com.dcim.site.rack;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Shallow rack DTO for API responses (current history row).
 */
public record RackDto(
		Long rackId,
		Long rackHistoryId,
		Long cageId,
		String rackName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static RackDto from(RackHistory history) {
		return new RackDto(
				history.getRackId(),
				history.getRackHistoryId(),
				history.getCageId(),
				history.getRackName(),
				AuditSlice.from(history));
	}
}
