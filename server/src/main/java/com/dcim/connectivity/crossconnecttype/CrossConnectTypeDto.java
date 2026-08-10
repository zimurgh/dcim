package com.dcim.connectivity.crossconnecttype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record CrossConnectTypeDto(
		Long crossConnectTypeId,
		Long crossConnectTypeHistoryId,
		String crossConnectTypeName,
		Long chargeTypeId,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static CrossConnectTypeDto from(CrossConnectTypeHistory history) {
		return new CrossConnectTypeDto(
				history.getCrossConnectTypeId(),
				history.getCrossConnectTypeHistoryId(),
				history.getCrossConnectTypeName(),
				history.getChargeTypeId(),
				AuditSlice.from(history));
	}
}
