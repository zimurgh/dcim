package com.dcim.site.rack;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/racks")
class RackController {

	private final RackService racks;

	RackController(RackService racks) {
		this.racks = racks;
	}

	@GetMapping
	List<RackDto> list(@RequestParam(required = false) Long cageId) {
		if (cageId != null) {
			return racks.listCurrentByCage(cageId);
		}
		return racks.listCurrent();
	}

	@GetMapping("/{rackId}")
	RackDto get(@PathVariable Long rackId) {
		return racks.findCurrent(rackId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rack not found: " + rackId));
	}

	@GetMapping("/{rackId}/history")
	List<RackDto> history(@PathVariable Long rackId) {
		List<RackDto> rows = racks.history(rackId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rack not found: " + rackId);
		}
		return rows;
	}
}
