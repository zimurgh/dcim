package com.dcim.site.datacenter;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/data-centers")
class DataCenterController {

	private final DataCenterService dataCenters;

	DataCenterController(DataCenterService dataCenters) {
		this.dataCenters = dataCenters;
	}

	@GetMapping
	List<DataCenterDto> list() {
		return dataCenters.listCurrent();
	}

	@GetMapping("/{dataCenterId}")
	DataCenterDto get(@PathVariable Long dataCenterId) {
		return AssetHttp.requireFound(dataCenters.findCurrent(dataCenterId), "Data center", dataCenterId);
	}

	@GetMapping("/{dataCenterId}/history")
	List<DataCenterDto> history(@PathVariable Long dataCenterId) {
		return AssetHttp.requireNonEmpty(dataCenters.history(dataCenterId), "Data center", dataCenterId);
	}
}
