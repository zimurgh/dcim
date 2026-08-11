package com.dcim.asset;

import java.util.List;

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
