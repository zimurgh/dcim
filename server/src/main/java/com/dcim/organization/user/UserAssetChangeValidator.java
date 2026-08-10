package com.dcim.organization.user;

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
class UserAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("userName", "isInitiator");

	private final UserHistoryRepository history;
	private final JsonPayloads payloads;

	UserAssetChangeValidator(UserHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "USER".equals(assetType);
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
				// User terminate is allowed even when the user has applied history rows (appliedBy is not blocked).
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				requireCurrentBase(command, issues);
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported user action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "userName", issues);
		String userName = PayloadValidation.textOrNull(body, "userName");
		if (userName != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(userName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"userName",
						"Another active user already uses name: " + userName));
			}
		}
	}

	private UserHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"User update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<UserHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND, null, "User history not found: " + command.baseHistoryId()));
			return null;
		}
		UserHistory base = found.get();
		if (!base.getUserId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to user " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale user baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}
}
