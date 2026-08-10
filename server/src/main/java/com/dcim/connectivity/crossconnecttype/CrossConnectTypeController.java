package com.dcim.connectivity.crossconnecttype;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
		return types.findCurrent(crossConnectTypeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Cross connect type not found: " + crossConnectTypeId));
	}

	@GetMapping("/{crossConnectTypeId}/history")
	List<CrossConnectTypeDto> history(@PathVariable Long crossConnectTypeId) {
		List<CrossConnectTypeDto> rows = types.history(crossConnectTypeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Cross connect type not found: " + crossConnectTypeId);
		}
		return rows;
	}
}
