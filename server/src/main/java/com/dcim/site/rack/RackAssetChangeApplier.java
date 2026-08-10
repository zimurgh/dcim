package com.dcim.site.rack;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.cage.CageIdentity;
import com.dcim.site.cage.CageIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackAssetChangeApplier implements AssetChangeApplier {

	private final RackIdentityRepository identities;
	private final RackHistoryRepository history;
	private final CageIdentityRepository cages;
	private final JsonPayloads payloads;

	RackAssetChangeApplier(
			RackIdentityRepository identities,
			RackHistoryRepository history,
			CageIdentityRepository cages,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.cages = cages;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported rack action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackName");
		CageIdentity cage = cages.findById(JsonPayloads.requiredLong(body, "cageId"))
				.orElseThrow(() -> new AssetApplyException("Cage not found for rack add"));
		RackIdentity identity = identities.saveAndFlush(new RackIdentity());
		RackHistory created = history.saveAndFlush(new RackHistory(
				identity,
				cage,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getRackId(),
				List.of(new AssetHistoryLink(created.getRackHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		RackHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackName");
		CageIdentity cage = body.hasNonNull("cageId")
				? cages.findById(JsonPayloads.requiredLong(body, "cageId"))
						.orElseThrow(() -> new AssetApplyException("Cage not found for rack update"))
				: prior.getCageIdentity();
		prior.close(command.validOn());
		RackHistory created = history.saveAndFlush(new RackHistory(
				prior.getRackIdentity(),
				cage,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		RackHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		RackHistory created = history.saveAndFlush(new RackHistory(
				prior.getRackIdentity(),
				prior.getCageIdentity(),
				prior.getRackName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private RackHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Rack update/terminate requires assetIdentityId and baseHistoryId");
		}
		RackHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Rack history not found: " + command.baseHistoryId()));
		if (!prior.getRackId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to rack " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale rack baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(RackHistory prior, RackHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getRackHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getRackHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getRackId(), List.copyOf(links));
	}
}
