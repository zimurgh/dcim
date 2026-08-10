package com.dcim.connectivity.cable;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.crossconnect.CrossConnectIdentityRepository;
import com.dcim.site.rackdeviceport.RackDevicePortIdentity;
import com.dcim.site.rackdeviceport.RackDevicePortIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CableAssetChangeApplier implements AssetChangeApplier {

	private final CableIdentityRepository identities;
	private final CableHistoryRepository history;
	private final RackDevicePortIdentityRepository ports;
	private final CrossConnectIdentityRepository crossConnects;
	private final JsonPayloads payloads;

	CableAssetChangeApplier(
			CableIdentityRepository identities,
			CableHistoryRepository history,
			RackDevicePortIdentityRepository ports,
			CrossConnectIdentityRepository crossConnects,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.ports = ports;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CABLE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported cable action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "cableName");
		RackDevicePortIdentity portA = requirePort(JsonPayloads.requiredLong(body, "portAId"), "portA");
		RackDevicePortIdentity portB = requirePort(JsonPayloads.requiredLong(body, "portBId"), "portB");
		requireDistinctPorts(portA, portB);
		CrossConnectIdentity crossConnect = optionalCrossConnect(JsonPayloads.longOrNull(body, "crossConnectId"));
		CableIdentity identity = identities.saveAndFlush(new CableIdentity());
		CableHistory created = history.saveAndFlush(new CableHistory(
				identity,
				name,
				portA,
				portB,
				crossConnect,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getCableId(),
				List.of(new AssetHistoryLink(created.getCableHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		CableHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "cableName");
		RackDevicePortIdentity portA = body.hasNonNull("portAId")
				? requirePort(JsonPayloads.requiredLong(body, "portAId"), "portA")
				: prior.getPortAIdentity();
		RackDevicePortIdentity portB = body.hasNonNull("portBId")
				? requirePort(JsonPayloads.requiredLong(body, "portBId"), "portB")
				: prior.getPortBIdentity();
		requireDistinctPorts(portA, portB);
		CrossConnectIdentity crossConnect = body.has("crossConnectId")
				? optionalCrossConnect(JsonPayloads.longOrNull(body, "crossConnectId"))
				: prior.getCrossConnectIdentity();
		prior.close(command.validOn());
		CableHistory created = history.saveAndFlush(new CableHistory(
				prior.getCableIdentity(),
				name,
				portA,
				portB,
				crossConnect,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		CableHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		CableHistory created = history.saveAndFlush(new CableHistory(
				prior.getCableIdentity(),
				prior.getCableName(),
				prior.getPortAIdentity(),
				prior.getPortBIdentity(),
				prior.getCrossConnectIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private RackDevicePortIdentity requirePort(Long portId, String role) {
		return ports.findById(portId)
				.orElseThrow(() -> new AssetApplyException(role + " not found: " + portId));
	}

	private CrossConnectIdentity optionalCrossConnect(Long crossConnectId) {
		if (crossConnectId == null) {
			return null;
		}
		return crossConnects.findById(crossConnectId)
				.orElseThrow(() -> new AssetApplyException("Cross connect not found: " + crossConnectId));
	}

	private static void requireDistinctPorts(RackDevicePortIdentity portA, RackDevicePortIdentity portB) {
		if (portA.getRackDevicePortId().equals(portB.getRackDevicePortId())) {
			throw new AssetApplyException("Cable ports must be distinct");
		}
	}

	private CableHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Cable update/terminate requires assetIdentityId and baseHistoryId");
		}
		CableHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Cable history not found: " + command.baseHistoryId()));
		if (!prior.getCableId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to cable " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale cable baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(CableHistory prior, CableHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getCableHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getCableHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getCableId(), List.copyOf(links));
	}
}
