package com.dcim.asset;

/**
 * Stable validation issue codes shared across domain validators.
 */
public final class ValidationCodes {

	public static final String UNKNOWN_FIELD = "UNKNOWN_FIELD";
	public static final String MISSING_FIELD = "MISSING_FIELD";
	public static final String INVALID_VALUE = "INVALID_VALUE";
	public static final String UNSUPPORTED_ACTION = "UNSUPPORTED_ACTION";
	public static final String MISSING_IDENTITY = "MISSING_IDENTITY";
	public static final String STALE_BASE = "STALE_BASE";
	public static final String IDENTITY_MISMATCH = "IDENTITY_MISMATCH";
	public static final String HISTORY_NOT_FOUND = "HISTORY_NOT_FOUND";
	public static final String REFERENCE_NOT_FOUND = "REFERENCE_NOT_FOUND";
	public static final String REFERENCE_NOT_ACTIVE = "REFERENCE_NOT_ACTIVE";
	public static final String NAME_CLASH = "NAME_CLASH";
	public static final String VALUE_CLASH = "VALUE_CLASH";
	public static final String ACTIVE_CHILDREN = "ACTIVE_CHILDREN";
	public static final String ACTIVE_REFERENCES = "ACTIVE_REFERENCES";
	public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";

	private ValidationCodes() {
	}
}
