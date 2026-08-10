package com.dcim.connectivity.cable;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record CableDto(
		Long cableId,
		Long cableHistoryId,
		String cableName,
		Long portAId,
		Long portBId,
		Long crossConnectId,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static CableDto from(CableHistory history) {
		return new CableDto(
				history.getCableId(),
				history.getCableHistoryId(),
				history.getCableName(),
				history.getPortAId(),
				history.getPortBId(),
				history.getCrossConnectId(),
				AuditSlice.from(history));
	}
}
