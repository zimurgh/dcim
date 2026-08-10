package com.dcim.organization.exchange;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeService {

	private final ExchangeHistoryRepository exchanges;

	ExchangeService(ExchangeHistoryRepository exchanges) {
		this.exchanges = exchanges;
	}

	@Transactional(readOnly = true)
	public List<ExchangeDto> listCurrent() {
		return exchanges.findCurrent().stream().map(ExchangeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<ExchangeDto> findCurrent(Long exchangeId) {
		return exchanges.findCurrentByExchangeId(exchangeId).map(ExchangeDto::from);
	}

	@Transactional(readOnly = true)
	public List<ExchangeDto> history(Long exchangeId) {
		return exchanges.findByExchangeIdentity_ExchangeIdOrderByExchangeHistoryIdAsc(exchangeId).stream()
				.map(ExchangeDto::from)
				.toList();
	}
}
