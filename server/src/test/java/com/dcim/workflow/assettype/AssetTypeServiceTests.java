package com.dcim.workflow.assettype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class AssetTypeServiceTests extends ChangeTestSupport {

	@Test
	void seedCatalogIsReadable() {
		assertThat(assetTypes.listCurrent())
				.extracting(AssetTypeDto::assetTypeCode)
				.contains("CAGE", "DATA_CENTER", "ASSET_TYPE", "USER");
		assertThat(assetTypes.findCurrentByCode("CAGE")).get()
				.extracting(AssetTypeDto::assetTypeName, AssetTypeDto::applyRank)
				.containsExactly("Cage", 50);
		assertThat(assetTypes.applyRank("CAGE")).isEqualTo(50);
		assertThat(assetTypes.applyRank("DATA_CENTER")).isGreaterThan(assetTypes.applyRank("CAGE"));
	}

	@Test
	void updatesAssetTypeNameAndRankThroughChangeWorkflow() {
		AssetTypeDto before = assetTypes.findCurrentByCode("CHARGE_TYPE").orElseThrow();

		applyUpdateCurrent(
				"ASSET_TYPE",
				before.assetTypeId(),
				json(Map.of(
						"assetTypeName", "Charge Type Renamed",
						"applyRank", 95)));

		AssetTypeDto current = assetTypes.findCurrent(before.assetTypeId()).orElseThrow();
		assertThat(current.assetTypeCode()).isEqualTo("CHARGE_TYPE");
		assertThat(current.assetTypeName()).isEqualTo("Charge Type Renamed");
		assertThat(current.applyRank()).isEqualTo(95);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(assetTypes.history(before.assetTypeId())).hasSize(2);
	}

	@Test
	void addsAssetTypeThroughChangeWorkflow() {
		Long id = applyAdd(
				"ASSET_TYPE",
				json(Map.of(
						"assetTypeCode", "CUSTOM_KIND",
						"assetTypeName", unique("Custom"),
						"applyRank", 88)))
				.assetIdentityId();

		AssetTypeDto current = assetTypes.findCurrent(id).orElseThrow();
		assertThat(current.assetTypeCode()).isEqualTo("CUSTOM_KIND");
		assertThat(current.applyRank()).isEqualTo(88);
		assertThat(current.status()).isEqualTo("Active");
	}
}
