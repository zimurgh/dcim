package com.dcim.connectivity.speed;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class SpeedAssetChangeApplier extends AbstractAssetChangeApplier<SpeedIdentity, SpeedHistory> {

	SpeedAssetChangeApplier(
			SpeedIdentityRepository identities,
			SpeedHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"SPEED",
				"speed",
				identities,
				history,
				payloads,
				SpeedIdentity::new,
				SpeedIdentity::getSpeedId,
				SpeedHistory::getSpeedId,
				SpeedHistory::getSpeedHistoryId);
	}

	@Override
	protected SpeedHistory createAdd(SpeedIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new SpeedHistory(
				identity,
				JsonPayloads.requiredText(body, "speedName"),
				requireSpeedType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected SpeedHistory createUpdate(SpeedHistory prior, JsonNode body, AssetApplyCommand command) {
		return new SpeedHistory(
				prior.getSpeedIdentity(),
				JsonPayloads.requiredText(body, "speedName"),
				requireSpeedType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected SpeedHistory createTerminate(SpeedHistory prior, AssetApplyCommand command) {
		return new SpeedHistory(
				prior.getSpeedIdentity(),
				prior.getSpeedName(),
				prior.getSpeedType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
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
}
