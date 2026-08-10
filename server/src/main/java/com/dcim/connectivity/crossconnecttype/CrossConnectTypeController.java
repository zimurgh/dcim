package com.dcim.connectivity.crossconnecttype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/cross-connect-types")
class CrossConnectTypeController {

	private final CrossConnectTypeService types;

	CrossConnectTypeController(CrossConnectTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<CrossConnectTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{crossConnectTypeId}")
	CrossConnectTypeDto get(@PathVariable Long crossConnectTypeId) {
		return AssetHttp.requireFound(types.findCurrent(crossConnectTypeId), "Cross connect type", crossConnectTypeId);
	}

	@GetMapping("/{crossConnectTypeId}/history")
	List<CrossConnectTypeDto> history(@PathVariable Long crossConnectTypeId) {
		return AssetHttp.requireNonEmpty(types.history(crossConnectTypeId), "Cross connect type", crossConnectTypeId);
	}
}
