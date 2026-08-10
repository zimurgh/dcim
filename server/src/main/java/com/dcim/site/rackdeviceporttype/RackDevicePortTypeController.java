package com.dcim.site.rackdeviceporttype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(types.findCurrent(rackDevicePortTypeId), "Rack device port type", rackDevicePortTypeId);
	}

	@GetMapping("/{rackDevicePortTypeId}/history")
	List<RackDevicePortTypeDto> history(@PathVariable Long rackDevicePortTypeId) {
		return AssetHttp.requireNonEmpty(types.history(rackDevicePortTypeId), "Rack device port type", rackDevicePortTypeId);
	}
}
