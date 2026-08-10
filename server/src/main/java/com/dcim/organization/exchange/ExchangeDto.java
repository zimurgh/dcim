package com.dcim.organization.exchange;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ExchangeDto(
		Long exchangeId,
		Long exchangeHistoryId,
		String exchangeName,
		String exchangeCode,
		String exchangeAbbreviation,
		ExchangeType exchangeType,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static ExchangeDto from(ExchangeHistory history) {
		return new ExchangeDto(
				history.getExchangeId(),
				history.getExchangeHistoryId(),
				history.getExchangeName(),
				history.getExchangeCode(),
				history.getExchangeAbbreviation(),
				history.getExchangeType(),
				AuditSlice.from(history));
	}
}
