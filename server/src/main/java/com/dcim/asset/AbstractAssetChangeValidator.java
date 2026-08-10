package com.dcim.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.jpa.repository.JpaRepository;

import tools.jackson.databind.JsonNode;

/**
 * Template for type-specific {@link AssetChangeValidator}s: action dispatch, unknown fields,
 * concurrency, and TERMINATE empty-payload checks. Subclasses supply ADD/UPDATE field rules and
 * TERMINATE dependency guards.
 *
 * @param <H> history entity extending {@link AuditHistory}
 */
public abstract class AbstractAssetChangeValidator<H extends AuditHistory> implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");

	private final String assetType;
	private final String assetLabel;
	private final Set<String> allowedFields;
	private final JpaRepository<H, Long> history;
	private final Function<H, Long> historyIdentityId;
	private final JsonPayloads payloads;

	protected AbstractAssetChangeValidator(
			String assetType,
			String assetLabel,
			Set<String> allowedFields,
			JpaRepository<H, Long> history,
			Function<H, Long> historyIdentityId,
			JsonPayloads payloads) {
		this.assetType = assetType;
		this.assetLabel = assetLabel;
		this.allowedFields = Set.copyOf(allowedFields);
		this.history = history;
		this.historyIdentityId = historyIdentityId;
		this.payloads = payloads;
	}

	@Override
	public final boolean supports(String assetType) {
		return this.assetType.equals(assetType);
	}

	@Override
	public final List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION,
					null,
					"Unsupported " + assetLabel + " action: " + command.action()));
			return issues;
		}

		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}

		return switch (command.action()) {
			case "ADD" -> {
				issues.addAll(PayloadValidation.unknownFields(body, allowedFields));
				validateAddOrUpdate(command, body, null, issues);
				yield issues;
			}
			case "UPDATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, allowedFields));
				H base = requireCurrentBase(command, issues);
				if (base != null) {
					validateAddOrUpdate(command, body, base, issues);
				}
				yield issues;
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				H base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminate(base, context, issues);
				}
				yield issues;
			}
			default -> issues;
		};
	}

	/**
	 * Field/shape/clash/reference checks for ADD and UPDATE.
	 *
	 * @param prior current history on UPDATE, or {@code null} on ADD
	 */
	protected abstract void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			H prior,
			List<ValidationIssue> issues);

	/**
	 * Dependency guards after concurrency has succeeded for TERMINATE. Default: no guards.
	 */
	protected void validateTerminate(H prior, ValidationContext context, List<ValidationIssue> issues) {
		// no-op
	}

	protected final H requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		return PayloadValidation.validateConcurrency(
				command.assetIdentityId(),
				command.baseHistoryId(),
				history::findById,
				historyIdentityId,
				AuditHistory::isCurrent,
				issues);
	}

	protected final JsonNode readBody(AssetValidateCommand command, List<ValidationIssue> issues) {
		try {
			return payloads.read(command.payloadJson());
		}
		catch (RuntimeException ex) {
			issues.add(new ValidationIssue(ValidationCodes.INVALID_PAYLOAD, ex.getMessage()));
			return null;
		}
	}

	protected final JsonPayloads payloads() {
		return payloads;
	}

	protected final String assetLabel() {
		return assetLabel;
	}
}
