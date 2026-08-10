package com.dcim.connectivity.chargetype;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/charge-types")
class ChargeTypeController {

	private final ChargeTypeService types;

	ChargeTypeController(ChargeTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<ChargeTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{chargeTypeId}")
	ChargeTypeDto get(@PathVariable Long chargeTypeId) {
		return types.findCurrent(chargeTypeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Charge type not found: " + chargeTypeId));
	}

	@GetMapping("/{chargeTypeId}/history")
	List<ChargeTypeDto> history(@PathVariable Long chargeTypeId) {
		List<ChargeTypeDto> rows = types.history(chargeTypeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge type not found: " + chargeTypeId);
		}
		return rows;
	}
}
