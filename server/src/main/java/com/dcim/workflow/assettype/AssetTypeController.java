package com.dcim.workflow.assettype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/asset-types")
class AssetTypeController {

	private final AssetTypeService types;

	AssetTypeController(AssetTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<AssetTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{assetTypeId}")
	AssetTypeDto get(@PathVariable Long assetTypeId) {
		return AssetHttp.requireFound(types.findCurrent(assetTypeId), "Asset type", assetTypeId);
	}

	@GetMapping("/{assetTypeId}/history")
	List<AssetTypeDto> history(@PathVariable Long assetTypeId) {
		return AssetHttp.requireNonEmpty(types.history(assetTypeId), "Asset type", assetTypeId);
	}
}
