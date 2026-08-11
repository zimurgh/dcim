package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class AssetApplyOrderTests {

	@Test
	void dependentsRankBeforeParents() {
		assertThat(AssetApplyOrder.rank(AssetType.RACK_DEVICE_PORT))
				.isLessThan(AssetApplyOrder.rank(AssetType.RACK_DEVICE));
		assertThat(AssetApplyOrder.rank(AssetType.RACK_DEVICE))
				.isLessThan(AssetApplyOrder.rank(AssetType.RACK));
		assertThat(AssetApplyOrder.rank(AssetType.RACK))
				.isLessThan(AssetApplyOrder.rank(AssetType.CAGE));
		assertThat(AssetApplyOrder.rank(AssetType.CAGE))
				.isLessThan(AssetApplyOrder.rank(AssetType.DATA_CENTER));
		assertThat(AssetApplyOrder.rank(AssetType.CABLE))
				.isLessThan(AssetApplyOrder.rank(AssetType.CROSS_CONNECT));
		assertThat(AssetApplyOrder.rank(AssetType.DOCUMENT))
				.isLessThan(AssetApplyOrder.rank(AssetType.CROSS_CONNECT));
		assertThat(AssetApplyOrder.rank(AssetType.MARKET_DATA_FEED))
				.isLessThan(AssetApplyOrder.rank(AssetType.CROSS_CONNECT));
		assertThat(AssetApplyOrder.rank(AssetType.CROSS_CONNECT))
				.isLessThan(AssetApplyOrder.rank(AssetType.FIRM));
		assertThat(AssetApplyOrder.rank(AssetType.FIRM))
				.isLessThan(AssetApplyOrder.rank(AssetType.USER));
	}

	@Test
	void everyAssetTypeHasARank() {
		for (AssetType type : AssetType.values()) {
			assertThat(AssetApplyOrder.rank(type)).isPositive();
		}
	}

	@Test
	void sortIsStableByChangeIdWithinSameRank() {
		List<AssetType> ordered = Arrays.stream(AssetType.values())
				.sorted(Comparator.comparingInt(AssetApplyOrder::rank).thenComparing(Enum::name))
				.toList();
		assertThat(ordered.getFirst()).isEqualTo(AssetType.RACK_DEVICE_PORT);
		assertThat(ordered.getLast()).isEqualTo(AssetType.USER);
	}
}
