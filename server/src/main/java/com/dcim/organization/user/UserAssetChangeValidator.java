package com.dcim.organization.user;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationIssue;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class UserAssetChangeValidator extends AbstractAssetChangeValidator<UserHistory> {

	private final UserHistoryRepository history;

	UserAssetChangeValidator(UserHistoryRepository history, JsonPayloads payloads) {
		super("USER", "user", Set.of("userName", "isInitiator"), history, UserHistory::getUserId, payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, UserHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "userName", issues);
		String userName = PayloadValidation.textOrNull(body, "userName");
		if (userName != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(userName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"userName",
						"Another active user already uses name: " + userName));
			}
		}
	}
}
