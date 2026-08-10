package com.dcim.connectivity.speed;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record SpeedDto(
		Long speedId,
		Long speedHistoryId,
		String speedName,
		SpeedType speedType,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static SpeedDto from(SpeedHistory history) {
		return new SpeedDto(
				history.getSpeedId(),
				history.getSpeedHistoryId(),
				history.getSpeedName(),
				history.getSpeedType(),
				AuditSlice.from(history));
	}
}
