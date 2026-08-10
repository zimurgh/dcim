package com.dcim.connectivity.latency;

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
class LatencyAssetChangeApplier implements AssetChangeApplier {

	private final LatencyIdentityRepository identities;
	private final LatencyHistoryRepository history;
	private final JsonPayloads payloads;

	LatencyAssetChangeApplier(
			LatencyIdentityRepository identities,
			LatencyHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "LATENCY".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported latency action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "latencyName");
		LatencyType type = requireLatencyType(body);
		LatencyIdentity identity = identities.saveAndFlush(new LatencyIdentity());
		LatencyHistory created = history.saveAndFlush(new LatencyHistory(
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
				identity.getLatencyId(),
				List.of(new AssetHistoryLink(created.getLatencyHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		LatencyHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "latencyName");
		LatencyType type = requireLatencyType(body);
		prior.close(command.validOn());
		LatencyHistory created = history.saveAndFlush(new LatencyHistory(
				prior.getLatencyIdentity(),
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
		LatencyHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		LatencyHistory created = history.saveAndFlush(new LatencyHistory(
				prior.getLatencyIdentity(),
				prior.getLatencyName(),
				prior.getLatencyType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
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

	private LatencyHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Latency update/terminate requires assetIdentityId and baseHistoryId");
		}
		LatencyHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Latency history not found: " + command.baseHistoryId()));
		if (!prior.getLatencyId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to latency " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale latency baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(LatencyHistory prior, LatencyHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getLatencyHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getLatencyHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getLatencyId(), List.copyOf(links));
	}
}
