package com.dcim.connectivity.speed;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(speeds.findCurrent(speedId), "Speed", speedId);
	}

	@GetMapping("/{speedId}/history")
	List<SpeedDto> history(@PathVariable Long speedId) {
		return AssetHttp.requireNonEmpty(speeds.history(speedId), "Speed", speedId);
	}
}
