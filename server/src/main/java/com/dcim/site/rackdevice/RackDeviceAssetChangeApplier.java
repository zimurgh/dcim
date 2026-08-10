package com.dcim.site.rackdevice;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rack.RackIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceAssetChangeApplier implements AssetChangeApplier {

	private final RackDeviceIdentityRepository identities;
	private final RackDeviceHistoryRepository history;
	private final RackIdentityRepository racks;
	private final JsonPayloads payloads;

	RackDeviceAssetChangeApplier(
			RackDeviceIdentityRepository identities,
			RackDeviceHistoryRepository history,
			RackIdentityRepository racks,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.racks = racks;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported rack device action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDeviceName");
		RackIdentity rack = racks.findById(JsonPayloads.requiredLong(body, "rackId"))
				.orElseThrow(() -> new AssetApplyException("Rack not found for rack device add"));
		RackDeviceIdentity identity = identities.saveAndFlush(new RackDeviceIdentity());
		RackDeviceHistory created = history.saveAndFlush(new RackDeviceHistory(
				identity,
				rack,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getRackDeviceId(),
				List.of(new AssetHistoryLink(created.getRackDeviceHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		RackDeviceHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDeviceName");
		RackIdentity rack = body.hasNonNull("rackId")
				? racks.findById(JsonPayloads.requiredLong(body, "rackId"))
						.orElseThrow(() -> new AssetApplyException("Rack not found for rack device update"))
				: prior.getRackIdentity();
		prior.close(command.validOn());
		RackDeviceHistory created = history.saveAndFlush(new RackDeviceHistory(
				prior.getRackDeviceIdentity(),
				rack,
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
		RackDeviceHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		RackDeviceHistory created = history.saveAndFlush(new RackDeviceHistory(
				prior.getRackDeviceIdentity(),
				prior.getRackIdentity(),
				prior.getRackDeviceName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private RackDeviceHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Rack device update/terminate requires assetIdentityId and baseHistoryId");
		}
		RackDeviceHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Rack device history not found: " + command.baseHistoryId()));
		if (!prior.getRackDeviceId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to rack device " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale rack device baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(RackDeviceHistory prior, RackDeviceHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getRackDeviceHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getRackDeviceHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getRackDeviceId(), List.copyOf(links));
	}
}
