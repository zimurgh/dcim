package com.dcim.connectivity.chargetype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ChargeTypeDto(
		Long chargeTypeId,
		Long chargeTypeHistoryId,
		String chargeTypeName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static ChargeTypeDto from(ChargeTypeHistory history) {
		return new ChargeTypeDto(
				history.getChargeTypeId(),
				history.getChargeTypeHistoryId(),
				history.getChargeTypeName(),
				AuditSlice.from(history));
	}
}
