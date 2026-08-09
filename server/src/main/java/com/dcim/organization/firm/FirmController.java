package com.dcim.organization.firm;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/firms")
class FirmController {

	private final FirmService firms;

	FirmController(FirmService firms) {
		this.firms = firms;
	}

	@GetMapping
	List<FirmDto> list() {
		return firms.listCurrent();
	}

	@GetMapping("/{firmId}")
	FirmDto get(@PathVariable Long firmId) {
		return firms.findCurrent(firmId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Firm not found: " + firmId));
	}

	@GetMapping("/{firmId}/history")
	List<FirmDto> history(@PathVariable Long firmId) {
		List<FirmDto> rows = firms.history(firmId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Firm not found: " + firmId);
		}
		return rows;
	}
}
