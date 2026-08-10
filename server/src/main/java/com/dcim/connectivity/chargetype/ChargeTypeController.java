package com.dcim.connectivity.chargetype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(types.findCurrent(chargeTypeId), "Charge type", chargeTypeId);
	}

	@GetMapping("/{chargeTypeId}/history")
	List<ChargeTypeDto> history(@PathVariable Long chargeTypeId) {
		return AssetHttp.requireNonEmpty(types.history(chargeTypeId), "Charge type", chargeTypeId);
	}
}
