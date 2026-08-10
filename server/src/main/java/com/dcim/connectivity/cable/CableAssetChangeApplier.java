package com.dcim.connectivity.cable;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.crossconnect.CrossConnectIdentityRepository;
import com.dcim.site.rackdeviceport.RackDevicePortIdentity;
import com.dcim.site.rackdeviceport.RackDevicePortIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CableAssetChangeApplier extends AbstractAssetChangeApplier<CableIdentity, CableHistory> {

	private final RackDevicePortIdentityRepository ports;
	private final CrossConnectIdentityRepository crossConnects;

	CableAssetChangeApplier(
			CableIdentityRepository identities,
			CableHistoryRepository history,
			RackDevicePortIdentityRepository ports,
			CrossConnectIdentityRepository crossConnects,
			JsonPayloads payloads) {
		super(
				"CABLE",
				"cable",
				identities,
				history,
				payloads,
				CableIdentity::new,
				CableIdentity::getCableId,
				CableHistory::getCableId,
				CableHistory::getCableHistoryId);
		this.ports = ports;
		this.crossConnects = crossConnects;
	}

	@Override
	protected CableHistory createAdd(CableIdentity identity, JsonNode body, AssetApplyCommand command) {
		RackDevicePortIdentity portA = requirePort(JsonPayloads.requiredLong(body, "portAId"), "portA");
		RackDevicePortIdentity portB = requirePort(JsonPayloads.requiredLong(body, "portBId"), "portB");
		requireDistinctPorts(portA, portB);
		CrossConnectIdentity crossConnect = optionalCrossConnect(JsonPayloads.longOrNull(body, "crossConnectId"));
		return new CableHistory(
				identity,
				JsonPayloads.requiredText(body, "cableName"),
				portA,
				portB,
				crossConnect,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CableHistory createUpdate(CableHistory prior, JsonNode body, AssetApplyCommand command) {
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
		return new CableHistory(
				prior.getCableIdentity(),
				JsonPayloads.requiredText(body, "cableName"),
				portA,
				portB,
				crossConnect,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CableHistory createTerminate(CableHistory prior, AssetApplyCommand command) {
		return new CableHistory(
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
				command.committedStatus());
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
}
