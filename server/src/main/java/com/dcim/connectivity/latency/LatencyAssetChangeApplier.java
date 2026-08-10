package com.dcim.connectivity.latency;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class LatencyAssetChangeApplier extends AbstractAssetChangeApplier<LatencyIdentity, LatencyHistory> {

	LatencyAssetChangeApplier(
			LatencyIdentityRepository identities,
			LatencyHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"LATENCY",
				"latency",
				identities,
				history,
				payloads,
				LatencyIdentity::new,
				LatencyIdentity::getLatencyId,
				LatencyHistory::getLatencyId,
				LatencyHistory::getLatencyHistoryId);
	}

	@Override
	protected LatencyHistory createAdd(LatencyIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new LatencyHistory(
				identity,
				JsonPayloads.requiredText(body, "latencyName"),
				requireLatencyType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected LatencyHistory createUpdate(LatencyHistory prior, JsonNode body, AssetApplyCommand command) {
		return new LatencyHistory(
				prior.getLatencyIdentity(),
				JsonPayloads.requiredText(body, "latencyName"),
				requireLatencyType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected LatencyHistory createTerminate(LatencyHistory prior, AssetApplyCommand command) {
		return new LatencyHistory(
				prior.getLatencyIdentity(),
				prior.getLatencyName(),
				prior.getLatencyType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private static LatencyType requireLatencyType(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "latencyType");
		try {
			return LatencyType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException("latencyType must be LL or ULL: " + raw);
		}
	}
}
