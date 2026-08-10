package com.dcim.site.rack;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(racks.findCurrent(rackId), "Rack", rackId);
	}

	@GetMapping("/{rackId}/history")
	List<RackDto> history(@PathVariable Long rackId) {
		return AssetHttp.requireNonEmpty(racks.history(rackId), "Rack", rackId);
	}
}
