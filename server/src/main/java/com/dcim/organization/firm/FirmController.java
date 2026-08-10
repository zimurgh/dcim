package com.dcim.organization.firm;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/firms")
class FirmController {

	private final FirmService firms;

	FirmController(FirmService firms) {
		this.firms = firms;
	}

	@GetMapping
	List<FirmDto> list() {
		return firms.listCurrent();
	}

	@GetMapping("/{firmId}")
	FirmDto get(@PathVariable Long firmId) {
		return AssetHttp.requireFound(firms.findCurrent(firmId), "Firm", firmId);
	}

	@GetMapping("/{firmId}/history")
	List<FirmDto> history(@PathVariable Long firmId) {
		return AssetHttp.requireNonEmpty(firms.history(firmId), "Firm", firmId);
	}
}
