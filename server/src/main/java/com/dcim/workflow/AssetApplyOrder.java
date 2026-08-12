package com.dcim.workflow;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class AssetApplyOrder {

	private static final Map<String, Integer> DEFAULT_RANKS = Map.ofEntries(
			Map.entry("RACK_DEVICE_PORT", 10),
			Map.entry("CABLE", 15),
			Map.entry("DOCUMENT", 20),
			Map.entry("MARKET_DATA_FEED", 20),
			Map.entry("RACK_DEVICE", 30),
			Map.entry("RACK", 40),
			Map.entry("CAGE", 50),
			Map.entry("CROSS_CONNECT", 60),
			Map.entry("DATA_CENTER", 70),
			Map.entry("RACK_DEVICE_PORT_TYPE", 80),
			Map.entry("RACK_DEVICE_TYPE", 80),
			Map.entry("ASSET_TYPE", 85),
			Map.entry("MARKET_DATA_FEED_TYPE", 90),
			Map.entry("CROSS_CONNECT_TYPE", 90),
			Map.entry("LATENCY", 90),
			Map.entry("SPEED", 90),
			Map.entry("CHARGE_TYPE", 90),
			Map.entry("MARKET_SEGMENT", 100),
			Map.entry("EXCHANGE", 100),
			Map.entry("FIRM", 110),
			Map.entry("USER", 120));

	private AssetApplyOrder() {
	}

	public static int rank(String assetTypeCode) {
		Integer rank = DEFAULT_RANKS.get(assetTypeCode);
		if (rank == null) {
			throw new IllegalArgumentException("Unknown asset type code: " + assetTypeCode);
		}
		return rank;
	}

	public static List<String> knownCodes() {
		return DEFAULT_RANKS.keySet().stream().sorted().toList();
	}

	public static Comparator<String> codeComparator() {
		return Comparator.comparingInt(AssetApplyOrder::rank).thenComparing(code -> code);
	}
}
