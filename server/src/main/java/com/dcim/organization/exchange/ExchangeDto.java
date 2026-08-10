package com.dcim.organization.exchange;

import java.time.Instant;
import java.time.LocalDate;

public record ExchangeDto(
		Long exchangeId,
		Long exchangeHistoryId,
		String exchangeName,
		String exchangeCode,
		String exchangeAbbreviation,
		ExchangeType exchangeType,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static ExchangeDto from(ExchangeHistory history) {
		return new ExchangeDto(
				history.getExchangeId(),
				history.getExchangeHistoryId(),
				history.getExchangeName(),
				history.getExchangeCode(),
				history.getExchangeAbbreviation(),
				history.getExchangeType(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
