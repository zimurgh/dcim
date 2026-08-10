package com.dcim.organization.marketsegment;

/**
 * Market segment catalog values for cross connects.
 */
public enum MarketSegmentType {
	EQUITIES_INDEX("Equities Index"),
	AGRICULTURAL_FUTURES("Agricultural Futures");

	private final String label;

	MarketSegmentType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static MarketSegmentType fromPayload(String raw) {
		String trimmed = raw.trim();
		String normalized = trimmed.toUpperCase().replace(' ', '_');
		for (MarketSegmentType type : values()) {
			if (type.name().equals(normalized) || type.label.equalsIgnoreCase(trimmed)) {
				return type;
			}
		}
		throw new IllegalArgumentException(raw);
	}
}
