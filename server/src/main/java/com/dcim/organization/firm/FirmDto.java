package com.dcim.organization.firm;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record FirmDto(
		Long firmId,
		Long firmHistoryId,
		String firmName,
		String parentFirmName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static FirmDto from(FirmHistory history) {
		return new FirmDto(
				history.getFirmId(),
				history.getFirmHistoryId(),
				history.getFirmName(),
				history.getParentFirmName(),
				AuditSlice.from(history));
	}
}
