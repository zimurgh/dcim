package com.dcim.site.rackdeviceport;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(ports.findCurrent(rackDevicePortId), "Rack device port", rackDevicePortId);
	}

	@GetMapping("/{rackDevicePortId}/history")
	List<RackDevicePortDto> history(@PathVariable Long rackDevicePortId) {
		return AssetHttp.requireNonEmpty(ports.history(rackDevicePortId), "Rack device port", rackDevicePortId);
	}
}
