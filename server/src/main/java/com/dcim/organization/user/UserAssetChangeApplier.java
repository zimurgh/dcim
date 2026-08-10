package com.dcim.organization.user;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class UserAssetChangeApplier implements AssetChangeApplier {

	private final UserIdentityRepository identities;
	private final UserHistoryRepository history;
	private final JsonPayloads payloads;

	UserAssetChangeApplier(
			UserIdentityRepository identities,
			UserHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "USER".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported user action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String userName = JsonPayloads.requiredText(body, "userName");
		boolean isInitiator = JsonPayloads.booleanOrDefault(body, "isInitiator", false);
		UserIdentity identity = identities.saveAndFlush(new UserIdentity());
		UserHistory created = history.saveAndFlush(new UserHistory(
				identity,
				userName,
				isInitiator,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getUserId(),
				List.of(new AssetHistoryLink(created.getUserHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		UserHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String userName = JsonPayloads.requiredText(body, "userName");
		boolean isInitiator = JsonPayloads.booleanOrDefault(body, "isInitiator", prior.isInitiator());
		prior.close(command.validOn());
		UserHistory created = history.saveAndFlush(new UserHistory(
				prior.getUserIdentity(),
				userName,
				isInitiator,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		UserHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		UserHistory created = history.saveAndFlush(new UserHistory(
				prior.getUserIdentity(),
				prior.getUserName(),
				prior.isInitiator(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private UserHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("User update/terminate requires assetIdentityId and baseHistoryId");
		}
		UserHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("User history not found: " + command.baseHistoryId()));
		if (!prior.getUserId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to user " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale user baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(UserHistory prior, UserHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getUserHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getUserHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getUserId(), List.copyOf(links));
	}
}
