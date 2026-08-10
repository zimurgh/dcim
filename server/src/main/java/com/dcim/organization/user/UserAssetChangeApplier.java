package com.dcim.organization.user;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class UserAssetChangeApplier extends AbstractAssetChangeApplier<UserIdentity, UserHistory> {

	UserAssetChangeApplier(
			UserIdentityRepository identities,
			UserHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"USER",
				"user",
				identities,
				history,
				payloads,
				UserIdentity::new,
				UserIdentity::getUserId,
				UserHistory::getUserId,
				UserHistory::getUserHistoryId);
	}

	@Override
	protected UserHistory createAdd(UserIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new UserHistory(
				identity,
				JsonPayloads.requiredText(body, "userName"),
				JsonPayloads.booleanOrDefault(body, "isInitiator", false),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected UserHistory createUpdate(UserHistory prior, JsonNode body, AssetApplyCommand command) {
		return new UserHistory(
				prior.getUserIdentity(),
				JsonPayloads.requiredText(body, "userName"),
				JsonPayloads.booleanOrDefault(body, "isInitiator", prior.isInitiator()),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected UserHistory createTerminate(UserHistory prior, AssetApplyCommand command) {
		return new UserHistory(
				prior.getUserIdentity(),
				prior.getUserName(),
				prior.isInitiator(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}
}
