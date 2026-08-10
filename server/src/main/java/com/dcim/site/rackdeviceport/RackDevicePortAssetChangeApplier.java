package com.dcim.site.rackdeviceport;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.rackdevice.RackDeviceIdentity;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDevicePortAssetChangeApplier implements AssetChangeApplier {

	private final RackDevicePortIdentityRepository identities;
	private final RackDevicePortHistoryRepository history;
	private final RackDeviceIdentityRepository devices;
	private final JsonPayloads payloads;

	RackDevicePortAssetChangeApplier(
			RackDevicePortIdentityRepository identities,
			RackDevicePortHistoryRepository history,
			RackDeviceIdentityRepository devices,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.devices = devices;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_PORT".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported rack device port action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDevicePortName");
		RackDeviceIdentity device = devices.findById(JsonPayloads.requiredLong(body, "rackDeviceId"))
				.orElseThrow(() -> new AssetApplyException("Rack device not found for port add"));
		RackDevicePortIdentity identity = identities.saveAndFlush(new RackDevicePortIdentity());
		RackDevicePortHistory created = history.saveAndFlush(new RackDevicePortHistory(
				identity,
				device,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getRackDevicePortId(),
				List.of(new AssetHistoryLink(created.getRackDevicePortHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		RackDevicePortHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "rackDevicePortName");
		RackDeviceIdentity device = body.hasNonNull("rackDeviceId")
				? devices.findById(JsonPayloads.requiredLong(body, "rackDeviceId"))
						.orElseThrow(() -> new AssetApplyException("Rack device not found for port update"))
				: prior.getRackDeviceIdentity();
		prior.close(command.validOn());
		RackDevicePortHistory created = history.saveAndFlush(new RackDevicePortHistory(
				prior.getRackDevicePortIdentity(),
				device,
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
		RackDevicePortHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		RackDevicePortHistory created = history.saveAndFlush(new RackDevicePortHistory(
				prior.getRackDevicePortIdentity(),
				prior.getRackDeviceIdentity(),
				prior.getRackDevicePortName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private RackDevicePortHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Port update/terminate requires assetIdentityId and baseHistoryId");
		}
		RackDevicePortHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Port history not found: " + command.baseHistoryId()));
		if (!prior.getRackDevicePortId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to port " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale port baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(RackDevicePortHistory prior, RackDevicePortHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getRackDevicePortHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getRackDevicePortHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getRackDevicePortId(), List.copyOf(links));
	}
}
