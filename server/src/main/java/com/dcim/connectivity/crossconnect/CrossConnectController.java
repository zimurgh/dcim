package com.dcim.connectivity.crossconnect;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/cross-connects")
class CrossConnectController {

	private final CrossConnectService crossConnects;

	CrossConnectController(CrossConnectService crossConnects) {
		this.crossConnects = crossConnects;
	}

	@GetMapping
	List<CrossConnectDto> list() {
		return crossConnects.listCurrent();
	}

	@GetMapping("/{crossConnectId}")
	CrossConnectDto get(@PathVariable Long crossConnectId) {
		return AssetHttp.requireFound(crossConnects.findCurrent(crossConnectId), "Cross connect", crossConnectId);
	}

	@GetMapping("/{crossConnectId}/history")
	List<CrossConnectDto> history(@PathVariable Long crossConnectId) {
		return AssetHttp.requireNonEmpty(crossConnects.history(crossConnectId), "Cross connect", crossConnectId);
	}
}
