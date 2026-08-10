package com.dcim.site.rack;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.cage.CageIdentity;
import com.dcim.site.cage.CageIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackAssetChangeApplier extends AbstractAssetChangeApplier<RackIdentity, RackHistory> {

	private final CageIdentityRepository cages;

	RackAssetChangeApplier(
			RackIdentityRepository identities,
			RackHistoryRepository history,
			CageIdentityRepository cages,
			JsonPayloads payloads) {
		super(
				"RACK",
				"rack",
				identities,
				history,
				payloads,
				RackIdentity::new,
				RackIdentity::getRackId,
				RackHistory::getRackId,
				RackHistory::getRackHistoryId);
		this.cages = cages;
	}

	@Override
	protected RackHistory createAdd(RackIdentity identity, JsonNode body, AssetApplyCommand command) {
		CageIdentity cage = requireCage(JsonPayloads.requiredLong(body, "cageId"), "add");
		return new RackHistory(
				identity,
				cage,
				JsonPayloads.requiredText(body, "rackName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackHistory createUpdate(RackHistory prior, JsonNode body, AssetApplyCommand command) {
		CageIdentity cage = body.hasNonNull("cageId")
				? requireCage(JsonPayloads.requiredLong(body, "cageId"), "update")
				: prior.getCageIdentity();
		return new RackHistory(
				prior.getRackIdentity(),
				cage,
				JsonPayloads.requiredText(body, "rackName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackHistory createTerminate(RackHistory prior, AssetApplyCommand command) {
		return new RackHistory(
				prior.getRackIdentity(),
				prior.getCageIdentity(),
				prior.getRackName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private CageIdentity requireCage(Long cageId, String action) {
		return cages.findById(cageId)
				.orElseThrow(() -> new AssetApplyException("Cage not found for rack " + action));
	}
}
