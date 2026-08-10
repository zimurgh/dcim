package com.dcim.workflow;

import java.util.Comparator;
import java.util.List;

/**
 * Apply dependents before parents within a Change Spec batch.
 */
final class AssetApplyOrder {

	private AssetApplyOrder() {
	}

	static int rank(AssetType type) {
		return switch (type) {
			case RACK_DEVICE_PORT -> 10;
			case CABLE -> 15;
			case DOCUMENT, MARKET_DATA_FEED -> 20;
			case RACK_DEVICE -> 30;
			case RACK -> 40;
			case CAGE -> 50;
			case CROSS_CONNECT -> 60;
			case DATA_CENTER -> 70;
			case RACK_DEVICE_PORT_TYPE, RACK_DEVICE_TYPE -> 80;
			case MARKET_DATA_FEED_TYPE, CROSS_CONNECT_TYPE, LATENCY, SPEED, CHARGE_TYPE -> 90;
			case MARKET_SEGMENT, EXCHANGE -> 100;
			case FIRM -> 110;
			case USER -> 120;
		};
	}

	static Comparator<ChangeStaged> comparator() {
		return Comparator
				.comparingInt((ChangeStaged staged) -> rank(staged.getAssetType()))
				.thenComparing(ChangeStaged::getChangeId);
	}

	static List<ChangeStaged> sort(List<ChangeStaged> staged) {
		return staged.stream().sorted(comparator()).toList();
	}
}
