package com.dcim.site.rackdevicetype;

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
class RackDeviceTypeAssetChangeApplier implements AssetChangeApplier {

	private final RackDeviceTypeIdentityRepository identities;
	private final RackDeviceTypeHistoryRepository history;
	private final JsonPayloads payloads;

	RackDeviceTypeAssetChangeApplier(
			RackDeviceTypeIdentityRepository identities,
			RackDeviceTypeHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_TYPE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported rack device type action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDeviceTypeName");
		RackDeviceTypeKind kind = requireKind(body);
		RackDeviceTypeIdentity identity = identities.saveAndFlush(new RackDeviceTypeIdentity());
		RackDeviceTypeHistory created = history.saveAndFlush(new RackDeviceTypeHistory(
				identity,
				name,
				kind,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getRackDeviceTypeId(),
				List.of(new AssetHistoryLink(created.getRackDeviceTypeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		RackDeviceTypeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDeviceTypeName");
		RackDeviceTypeKind kind = requireKind(body);
		prior.close(command.validOn());
		RackDeviceTypeHistory created = history.saveAndFlush(new RackDeviceTypeHistory(
				prior.getRackDeviceTypeIdentity(),
				name,
				kind,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		RackDeviceTypeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		RackDeviceTypeHistory created = history.saveAndFlush(new RackDeviceTypeHistory(
				prior.getRackDeviceTypeIdentity(),
				prior.getRackDeviceTypeName(),
				prior.getRackDeviceTypeKind(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private static RackDeviceTypeKind requireKind(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "rackDeviceTypeKind");
		try {
			return RackDeviceTypeKind.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException(
					"rackDeviceTypeKind must be Patch Panel, Extranet Switch, Matrix Switch, or Tap: " + raw);
		}
	}

	private RackDeviceTypeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Rack device type update/terminate requires assetIdentityId and baseHistoryId");
		}
		RackDeviceTypeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Rack device type history not found: " + command.baseHistoryId()));
		if (!prior.getRackDeviceTypeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to rack device type " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale rack device type baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(RackDeviceTypeHistory prior, RackDeviceTypeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getRackDeviceTypeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getRackDeviceTypeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getRackDeviceTypeId(), List.copyOf(links));
	}
}
