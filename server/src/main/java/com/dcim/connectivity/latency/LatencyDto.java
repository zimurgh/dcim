package com.dcim.connectivity.latency;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record LatencyDto(
		Long latencyId,
		Long latencyHistoryId,
		String latencyName,
		LatencyType latencyType,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static LatencyDto from(LatencyHistory history) {
		return new LatencyDto(
				history.getLatencyId(),
				history.getLatencyHistoryId(),
				history.getLatencyName(),
				history.getLatencyType(),
				AuditSlice.from(history));
	}
}
