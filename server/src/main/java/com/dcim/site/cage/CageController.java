package com.dcim.site.cage;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cages")
class CageController {

	private final CageService cages;

	CageController(CageService cages) {
		this.cages = cages;
	}

	@GetMapping
	List<CageDto> list(@RequestParam(required = false) Long dataCenterId) {
		if (dataCenterId != null) {
			return cages.listCurrentByDataCenter(dataCenterId);
		}
		return cages.listCurrent();
	}

	@GetMapping("/{cageId}")
	CageDto get(@PathVariable Long cageId) {
		return cages.findCurrent(cageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found: " + cageId));
	}

	@GetMapping("/{cageId}/history")
	List<CageDto> history(@PathVariable Long cageId) {
		List<CageDto> rows = cages.history(cageId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cage not found: " + cageId);
		}
		return rows;
	}
}
