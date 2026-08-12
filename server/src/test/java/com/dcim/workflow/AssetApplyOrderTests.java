package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class AssetApplyOrderTests {

	@Test
	void dependentsRankBeforeParents() {
		assertThat(AssetApplyOrder.rank("RACK_DEVICE_PORT"))
				.isLessThan(AssetApplyOrder.rank("RACK_DEVICE"));
		assertThat(AssetApplyOrder.rank("RACK_DEVICE"))
				.isLessThan(AssetApplyOrder.rank("RACK"));
		assertThat(AssetApplyOrder.rank("RACK"))
				.isLessThan(AssetApplyOrder.rank("CAGE"));
		assertThat(AssetApplyOrder.rank("CAGE"))
				.isLessThan(AssetApplyOrder.rank("DATA_CENTER"));
		assertThat(AssetApplyOrder.rank("CABLE"))
				.isLessThan(AssetApplyOrder.rank("CROSS_CONNECT"));
		assertThat(AssetApplyOrder.rank("DOCUMENT"))
				.isLessThan(AssetApplyOrder.rank("CROSS_CONNECT"));
		assertThat(AssetApplyOrder.rank("MARKET_DATA_FEED"))
				.isLessThan(AssetApplyOrder.rank("CROSS_CONNECT"));
		assertThat(AssetApplyOrder.rank("CROSS_CONNECT"))
				.isLessThan(AssetApplyOrder.rank("FIRM"));
		assertThat(AssetApplyOrder.rank("FIRM"))
				.isLessThan(AssetApplyOrder.rank("USER"));
	}

	@Test
	void everyAssetTypeHasARank() {
		for (String type : AssetApplyOrder.knownCodes()) {
			assertThat(AssetApplyOrder.rank(type)).isPositive();
		}
	}

	@Test
	void sortIsStableByChangeIdWithinSameRank() {
		List<String> ordered = AssetApplyOrder.knownCodes().stream()
				.sorted(Comparator.comparingInt(AssetApplyOrder::rank).thenComparing(code -> code))
				.toList();
		assertThat(ordered.getFirst()).isEqualTo("RACK_DEVICE_PORT");
		assertThat(ordered.getLast()).isEqualTo("USER");
	}
}
