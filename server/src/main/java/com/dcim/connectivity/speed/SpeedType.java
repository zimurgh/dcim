package com.dcim.connectivity.speed;

/**
 * Speed tier for a cross connect: 1G or 10G.
 */
public enum SpeedType {
	ONE_G("1G"),
	TEN_G("10G");

	private final String code;

	SpeedType(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static SpeedType fromPayload(String raw) {
		String normalized = raw.trim().toUpperCase();
		for (SpeedType type : values()) {
			if (type.name().equals(normalized) || type.code.equals(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException(raw);
	}
}
