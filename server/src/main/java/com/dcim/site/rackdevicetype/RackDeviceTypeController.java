package com.dcim.site.rackdevicetype;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rack-device-types")
class RackDeviceTypeController {

	private final RackDeviceTypeService types;

	RackDeviceTypeController(RackDeviceTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<RackDeviceTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{rackDeviceTypeId}")
	RackDeviceTypeDto get(@PathVariable Long rackDeviceTypeId) {
		return types.findCurrent(rackDeviceTypeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Rack device type not found: " + rackDeviceTypeId));
	}

	@GetMapping("/{rackDeviceTypeId}/history")
	List<RackDeviceTypeDto> history(@PathVariable Long rackDeviceTypeId) {
		List<RackDeviceTypeDto> rows = types.history(rackDeviceTypeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Rack device type not found: " + rackDeviceTypeId);
		}
		return rows;
	}
}
