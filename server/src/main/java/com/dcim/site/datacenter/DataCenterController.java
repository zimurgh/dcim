package com.dcim.site.datacenter;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
		return dataCenters.findCurrent(dataCenterId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Data center not found: " + dataCenterId));
	}

	@GetMapping("/{dataCenterId}/history")
	List<DataCenterDto> history(@PathVariable Long dataCenterId) {
		List<DataCenterDto> rows = dataCenters.history(dataCenterId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data center not found: " + dataCenterId);
		}
		return rows;
	}
}
