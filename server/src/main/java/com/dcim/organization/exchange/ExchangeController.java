package com.dcim.organization.exchange;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/exchanges")
class ExchangeController {

	private final ExchangeService exchanges;

	ExchangeController(ExchangeService exchanges) {
		this.exchanges = exchanges;
	}

	@GetMapping
	List<ExchangeDto> list() {
		return exchanges.listCurrent();
	}

	@GetMapping("/{exchangeId}")
	ExchangeDto get(@PathVariable Long exchangeId) {
		return exchanges.findCurrent(exchangeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Exchange not found: " + exchangeId));
	}

	@GetMapping("/{exchangeId}/history")
	List<ExchangeDto> history(@PathVariable Long exchangeId) {
		List<ExchangeDto> rows = exchanges.history(exchangeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange not found: " + exchangeId);
		}
		return rows;
	}
}
