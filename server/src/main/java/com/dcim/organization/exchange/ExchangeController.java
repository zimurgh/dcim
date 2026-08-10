package com.dcim.organization.exchange;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(exchanges.findCurrent(exchangeId), "Exchange", exchangeId);
	}

	@GetMapping("/{exchangeId}/history")
	List<ExchangeDto> history(@PathVariable Long exchangeId) {
		return AssetHttp.requireNonEmpty(exchanges.history(exchangeId), "Exchange", exchangeId);
	}
}
