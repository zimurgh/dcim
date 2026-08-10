package com.dcim.site.rackdeviceporttype;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rack-device-port-types")
class RackDevicePortTypeController {

	private final RackDevicePortTypeService types;

	RackDevicePortTypeController(RackDevicePortTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<RackDevicePortTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{rackDevicePortTypeId}")
	RackDevicePortTypeDto get(@PathVariable Long rackDevicePortTypeId) {
		return types.findCurrent(rackDevicePortTypeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Rack device port type not found: " + rackDevicePortTypeId));
	}

	@GetMapping("/{rackDevicePortTypeId}/history")
	List<RackDevicePortTypeDto> history(@PathVariable Long rackDevicePortTypeId) {
		List<RackDevicePortTypeDto> rows = types.history(rackDevicePortTypeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Rack device port type not found: " + rackDevicePortTypeId);
		}
		return rows;
	}
}
