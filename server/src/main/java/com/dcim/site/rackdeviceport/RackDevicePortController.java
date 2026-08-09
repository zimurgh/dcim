package com.dcim.site.rackdeviceport;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rack-device-ports")
class RackDevicePortController {

	private final RackDevicePortService ports;

	RackDevicePortController(RackDevicePortService ports) {
		this.ports = ports;
	}

	@GetMapping
	List<RackDevicePortDto> list(@RequestParam(required = false) Long rackDeviceId) {
		if (rackDeviceId != null) {
			return ports.listCurrentByRackDevice(rackDeviceId);
		}
		return ports.listCurrent();
	}

	@GetMapping("/{rackDevicePortId}")
	RackDevicePortDto get(@PathVariable Long rackDevicePortId) {
		return ports.findCurrent(rackDevicePortId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Rack device port not found: " + rackDevicePortId));
	}

	@GetMapping("/{rackDevicePortId}/history")
	List<RackDevicePortDto> history(@PathVariable Long rackDevicePortId) {
		List<RackDevicePortDto> rows = ports.history(rackDevicePortId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Rack device port not found: " + rackDevicePortId);
		}
		return rows;
	}
}
