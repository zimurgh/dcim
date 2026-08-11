package com.dcim.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import tools.jackson.databind.JsonNode;

public final class PayloadValidation {

	private PayloadValidation() {
	}

	public static List<ValidationIssue> unknownFields(JsonNode body, Set<String> allowed) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (body == null || !body.isObject()) {
			return issues;
		}
		for (Map.Entry<String, JsonNode> entry : body.properties()) {
			if (!allowed.contains(entry.getKey())) {
				issues.add(ValidationIssue.of(
						ValidationCodes.UNKNOWN_FIELD,
						entry.getKey(),
						"Unknown field: " + entry.getKey()));
			}
		}
		return issues;
	}

	public static void requireText(JsonNode body, String field, List<ValidationIssue> issues) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull() || node.asString().isBlank()) {
			issues.add(ValidationIssue.of(ValidationCodes.MISSING_FIELD, field, "Payload missing " + field));
		}
	}

	public static String textOrNull(JsonNode body, String field) {
		return JsonPayloads.textOrNull(body, field);
	}

	public static void requireLong(JsonNode body, String field, List<ValidationIssue> issues) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull() || !node.canConvertToLong()) {
			issues.add(ValidationIssue.of(ValidationCodes.MISSING_FIELD, field, "Payload missing " + field));
		}
	}

	public static Long longOrNull(JsonNode body, String field, List<ValidationIssue> issues) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.canConvertToLong()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE, field, "Payload field " + field + " must be a number"));
			return null;
		}
		return node.asLong();
	}

	public static Long requiredLong(JsonNode body, String field, List<ValidationIssue> issues) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull()) {
			issues.add(ValidationIssue.of(ValidationCodes.MISSING_FIELD, field, "Payload missing " + field));
			return null;
		}
		return longOrNull(body, field, issues);
	}

	public static boolean isActive(AuditHistory history) {
		return history != null && history.isCurrent() && "Active".equals(history.getStatus());
	}

	public static boolean isActiveStatus(String status) {
		return "Active".equals(status);
	}

	public static void requireActiveReference(String field, Long id, String status, List<ValidationIssue> issues) {
		if (status == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND, field, field + " not found: " + id, id));
		}
		else if (!isActiveStatus(status)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE, field, field + " is not active: " + id, id));
		}
	}

	public static String resolveRequiredText(
			JsonNode body, String field, boolean isAdd, String priorValue, List<ValidationIssue> issues) {
		boolean present = body.hasNonNull(field);
		if (!isAdd && !present) {
			return priorValue;
		}
		String value = textOrNull(body, field);
		if (value == null) {
			issues.add(new ValidationIssue(ValidationCodes.MISSING_FIELD, field, "Payload missing " + field));
			return priorValue;
		}
		return value;
	}

	public static Long resolveRequiredReference(
			JsonNode body,
			String field,
			boolean isAdd,
			Long priorId,
			List<ValidationIssue> issues,
			Function<Long, String> statusLookup) {
		boolean present = body.hasNonNull(field);
		if (!isAdd && !present) {
			return priorId;
		}
		Long id = longOrNull(body, field, issues);
		if (id == null) {
			if (isAdd) {
				issues.add(new ValidationIssue(ValidationCodes.MISSING_FIELD, field, "Payload missing " + field));
			}
			return priorId;
		}
		requireActiveReference(field, id, statusLookup.apply(id), issues);
		return id;
	}

	public static void validateOptionalReference(
			JsonNode body, String field, List<ValidationIssue> issues, Function<Long, String> statusLookup) {
		if (!body.has(field)) {
			return;
		}
		Long id = longOrNull(body, field, issues);
		if (id != null) {
			requireActiveReference(field, id, statusLookup.apply(id), issues);
		}
	}

	public static <T> T validateConcurrency(
			Long assetIdentityId,
			Long baseHistoryId,
			Function<Long, Optional<T>> historyLookup,
			Function<T, Long> identityIdExtractor,
			Predicate<T> isCurrent,
			List<ValidationIssue> issues) {
		if (assetIdentityId == null || baseHistoryId == null) {
			issues.add(new ValidationIssue(
					ValidationCodes.MISSING_IDENTITY,
					"assetIdentityId and baseHistoryId are both required for this action"));
			return null;
		}
		Optional<T> found = historyLookup.apply(baseHistoryId);
		if (found.isEmpty()) {
			issues.add(new ValidationIssue(ValidationCodes.HISTORY_NOT_FOUND, "History not found: " + baseHistoryId));
			return null;
		}
		T base = found.get();
		if (!identityIdExtractor.apply(base).equals(assetIdentityId)) {
			issues.add(new ValidationIssue(
					ValidationCodes.IDENTITY_MISMATCH,
					"baseHistoryId " + baseHistoryId + " does not belong to identity " + assetIdentityId));
			return null;
		}
		if (!isCurrent.test(base)) {
			issues.add(new ValidationIssue(ValidationCodes.STALE_BASE, "Stale baseHistoryId: " + baseHistoryId));
			return null;
		}
		return base;
	}
}
