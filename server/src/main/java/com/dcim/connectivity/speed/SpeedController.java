package com.dcim.connectivity.speed;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/speeds")
class SpeedController {

	private final SpeedService speeds;

	SpeedController(SpeedService speeds) {
		this.speeds = speeds;
	}

	@GetMapping
	List<SpeedDto> list() {
		return speeds.listCurrent();
	}

	@GetMapping("/{speedId}")
	SpeedDto get(@PathVariable Long speedId) {
		return speeds.findCurrent(speedId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Speed not found: " + speedId));
	}

	@GetMapping("/{speedId}/history")
	List<SpeedDto> history(@PathVariable Long speedId) {
		List<SpeedDto> rows = speeds.history(speedId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Speed not found: " + speedId);
		}
		return rows;
	}
}
