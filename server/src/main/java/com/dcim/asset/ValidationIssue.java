package com.dcim.asset;

import java.util.List;

/**
 * Structured, diagnosable validation finding for a staged change.
 *
 * @param code stable machine-readable code (e.g. {@code UNKNOWN_FIELD}, {@code NAME_CLASH})
 * @param field optional JSON payload field
 * @param message human-readable explanation
 * @param relatedIdentityIds optional conflicting or blocking asset identity ids
 */
public record ValidationIssue(
		String code,
		String field,
		String message,
		List<Long> relatedIdentityIds) {

	public ValidationIssue(String code, String field, String message) {
		this(code, field, message, List.of());
	}

	public ValidationIssue(String code, String message) {
		this(code, null, message, List.of());
	}

	public static ValidationIssue of(String code, String field, String message, Long... relatedIds) {
		return new ValidationIssue(code, field, message, relatedIds == null ? List.of() : List.of(relatedIds));
	}
}
