package com.dcim.connectivity.cable;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cables")
class CableController {

	private final CableService cables;

	CableController(CableService cables) {
		this.cables = cables;
	}

	@GetMapping
	List<CableDto> list(@RequestParam(required = false) Long crossConnectId) {
		if (crossConnectId != null) {
			return cables.listCurrentByCrossConnect(crossConnectId);
		}
		return cables.listCurrent();
	}

	@GetMapping("/{cableId}")
	CableDto get(@PathVariable Long cableId) {
		return cables.findCurrent(cableId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cable not found: " + cableId));
	}

	@GetMapping("/{cableId}/history")
	List<CableDto> history(@PathVariable Long cableId) {
		List<CableDto> rows = cables.history(cableId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cable not found: " + cableId);
		}
		return rows;
	}
}
