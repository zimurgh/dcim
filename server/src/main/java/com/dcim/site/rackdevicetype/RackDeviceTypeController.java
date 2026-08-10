package com.dcim.site.rackdevicetype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(types.findCurrent(rackDeviceTypeId), "Rack device type", rackDeviceTypeId);
	}

	@GetMapping("/{rackDeviceTypeId}/history")
	List<RackDeviceTypeDto> history(@PathVariable Long rackDeviceTypeId) {
		return AssetHttp.requireNonEmpty(types.history(rackDeviceTypeId), "Rack device type", rackDeviceTypeId);
	}
}
