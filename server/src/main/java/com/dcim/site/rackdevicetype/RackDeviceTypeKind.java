package com.dcim.site.rackdevicetype;

public enum RackDeviceTypeKind {
	PATCH_PANEL("Patch Panel"),
	EXTRANET_SWITCH("Extranet Switch"),
	MATRIX_SWITCH("Matrix Switch"),
	TAP("Tap");

	private final String label;

	RackDeviceTypeKind(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static RackDeviceTypeKind fromPayload(String raw) {
		String trimmed = raw.trim();
		String normalized = trimmed.toUpperCase().replace(' ', '_');
		for (RackDeviceTypeKind type : values()) {
			if (type.name().equals(normalized) || type.label.equalsIgnoreCase(trimmed)) {
				return type;
			}
		}
		throw new IllegalArgumentException(raw);
	}
}
