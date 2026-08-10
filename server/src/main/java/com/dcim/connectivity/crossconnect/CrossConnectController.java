package com.dcim.connectivity.crossconnect;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cross-connects")
class CrossConnectController {

	private final CrossConnectService crossConnects;

	CrossConnectController(CrossConnectService crossConnects) {
		this.crossConnects = crossConnects;
	}

	@GetMapping
	List<CrossConnectDto> list() {
		return crossConnects.listCurrent();
	}

	@GetMapping("/{crossConnectId}")
	CrossConnectDto get(@PathVariable Long crossConnectId) {
		return crossConnects.findCurrent(crossConnectId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Cross connect not found: " + crossConnectId));
	}

	@GetMapping("/{crossConnectId}/history")
	List<CrossConnectDto> history(@PathVariable Long crossConnectId) {
		List<CrossConnectDto> rows = crossConnects.history(crossConnectId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cross connect not found: " + crossConnectId);
		}
		return rows;
	}
}
