package com.dcim.connectivity.speed;

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
class SpeedAssetChangeApplier implements AssetChangeApplier {

	private final SpeedIdentityRepository identities;
	private final SpeedHistoryRepository history;
	private final JsonPayloads payloads;

	SpeedAssetChangeApplier(
			SpeedIdentityRepository identities,
			SpeedHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "SPEED".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported speed action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "speedName");
		SpeedType type = requireSpeedType(body);
		SpeedIdentity identity = identities.saveAndFlush(new SpeedIdentity());
		SpeedHistory created = history.saveAndFlush(new SpeedHistory(
				identity,
				name,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getSpeedId(),
				List.of(new AssetHistoryLink(created.getSpeedHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		SpeedHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "speedName");
		SpeedType type = requireSpeedType(body);
		prior.close(command.validOn());
		SpeedHistory created = history.saveAndFlush(new SpeedHistory(
				prior.getSpeedIdentity(),
				name,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		SpeedHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		SpeedHistory created = history.saveAndFlush(new SpeedHistory(
				prior.getSpeedIdentity(),
				prior.getSpeedName(),
				prior.getSpeedType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private static SpeedType requireSpeedType(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "speedType");
		try {
			return SpeedType.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException("speedType must be 1G or 10G: " + raw);
		}
	}

	private SpeedHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Speed update/terminate requires assetIdentityId and baseHistoryId");
		}
		SpeedHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Speed history not found: " + command.baseHistoryId()));
		if (!prior.getSpeedId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to speed " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale speed baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(SpeedHistory prior, SpeedHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getSpeedHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getSpeedHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getSpeedId(), List.copyOf(links));
	}
}
