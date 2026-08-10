package com.dcim.connectivity.cable;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/cables")
class CableController {

	private final CableService cables;

	CableController(CableService cables) {
		this.cables = cables;
	}

	@GetMapping
	List<CableDto> list(@RequestParam(required = false) Long crossConnectId) {
		if (crossConnectId != null) {
			return cables.listCurrentByCrossConnect(crossConnectId);
		}
		return cables.listCurrent();
	}

	@GetMapping("/{cableId}")
	CableDto get(@PathVariable Long cableId) {
		return AssetHttp.requireFound(cables.findCurrent(cableId), "Cable", cableId);
	}

	@GetMapping("/{cableId}/history")
	List<CableDto> history(@PathVariable Long cableId) {
		return AssetHttp.requireNonEmpty(cables.history(cableId), "Cable", cableId);
	}
}
