package com.dcim.site.rackdevice;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/rack-devices")
class RackDeviceController {

	private final RackDeviceService rackDevices;

	RackDeviceController(RackDeviceService rackDevices) {
		this.rackDevices = rackDevices;
	}

	@GetMapping
	List<RackDeviceDto> list(@RequestParam(required = false) Long rackId) {
		if (rackId != null) {
			return rackDevices.listCurrentByRack(rackId);
		}
		return rackDevices.listCurrent();
	}

	@GetMapping("/{rackDeviceId}")
	RackDeviceDto get(@PathVariable Long rackDeviceId) {
		return AssetHttp.requireFound(rackDevices.findCurrent(rackDeviceId), "Rack device", rackDeviceId);
	}

	@GetMapping("/{rackDeviceId}/history")
	List<RackDeviceDto> history(@PathVariable Long rackDeviceId) {
		return AssetHttp.requireNonEmpty(rackDevices.history(rackDeviceId), "Rack device", rackDeviceId);
	}
}
