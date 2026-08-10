package com.dcim.site.rackdeviceporttype;

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
class RackDevicePortTypeAssetChangeApplier implements AssetChangeApplier {

	private final RackDevicePortTypeIdentityRepository identities;
	private final RackDevicePortTypeHistoryRepository history;
	private final JsonPayloads payloads;

	RackDevicePortTypeAssetChangeApplier(
			RackDevicePortTypeIdentityRepository identities,
			RackDevicePortTypeHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_PORT_TYPE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported rack device port type action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDevicePortTypeName");
		RackDevicePortTypeIdentity identity = identities.saveAndFlush(new RackDevicePortTypeIdentity());
		RackDevicePortTypeHistory created = history.saveAndFlush(new RackDevicePortTypeHistory(
				identity,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getRackDevicePortTypeId(),
				List.of(new AssetHistoryLink(
						created.getRackDevicePortTypeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		RackDevicePortTypeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDevicePortTypeName");
		prior.close(command.validOn());
		RackDevicePortTypeHistory created = history.saveAndFlush(new RackDevicePortTypeHistory(
				prior.getRackDevicePortTypeIdentity(),
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
		RackDevicePortTypeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		RackDevicePortTypeHistory created = history.saveAndFlush(new RackDevicePortTypeHistory(
				prior.getRackDevicePortTypeIdentity(),
				prior.getRackDevicePortTypeName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private RackDevicePortTypeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Rack device port type update/terminate requires assetIdentityId and baseHistoryId");
		}
		RackDevicePortTypeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Rack device port type history not found: " + command.baseHistoryId()));
		if (!prior.getRackDevicePortTypeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to rack device port type " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale rack device port type baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(RackDevicePortTypeHistory prior, RackDevicePortTypeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(
				prior.getRackDevicePortTypeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(
				created.getRackDevicePortTypeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getRackDevicePortTypeId(), List.copyOf(links));
	}
}
