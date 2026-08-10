package com.dcim.organization.exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dcim.asset.AssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ExchangeAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS =
			Set.of("exchangeName", "exchangeCode", "exchangeAbbreviation", "exchangeType");

	private final ExchangeHistoryRepository history;
	private final JsonPayloads payloads;

	ExchangeAssetChangeValidator(ExchangeHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "EXCHANGE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, issues);
			case "UPDATE" -> {
				requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				requireCurrentBase(command, issues);
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported exchange action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "exchangeName", issues);
		PayloadValidation.requireText(body, "exchangeCode", issues);
		PayloadValidation.requireText(body, "exchangeAbbreviation", issues);
		validateExchangeType(body, issues);

		String exchangeName = PayloadValidation.textOrNull(body, "exchangeName");
		if (exchangeName != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(exchangeName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"exchangeName",
						"Another active exchange already uses name: " + exchangeName));
			}
		}
	}

	private static void validateExchangeType(JsonNode body, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "exchangeType", issues);
		String raw = PayloadValidation.textOrNull(body, "exchangeType");
		if (raw == null) {
			return;
		}
		try {
			ExchangeType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE,
					"exchangeType",
					"exchangeType must be one of OPTIONS, EQUITIES, FUTURES: " + raw));
		}
	}

	private ExchangeHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Exchange update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<ExchangeHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Exchange history not found: " + command.baseHistoryId()));
			return null;
		}
		ExchangeHistory base = found.get();
		if (!base.getExchangeId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to exchange " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale exchange baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}
}
