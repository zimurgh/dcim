package com.dcim.connectivity.crossconnecttype;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;
import com.dcim.connectivity.chargetype.ChargeTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectTypeAssetChangeApplier implements AssetChangeApplier {

	private final CrossConnectTypeIdentityRepository identities;
	private final CrossConnectTypeHistoryRepository history;
	private final ChargeTypeIdentityRepository chargeTypes;
	private final JsonPayloads payloads;

	CrossConnectTypeAssetChangeApplier(
			CrossConnectTypeIdentityRepository identities,
			CrossConnectTypeHistoryRepository history,
			ChargeTypeIdentityRepository chargeTypes,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.chargeTypes = chargeTypes;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CROSS_CONNECT_TYPE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported cross connect type action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "crossConnectTypeName");
		ChargeTypeIdentity chargeType = optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"));
		CrossConnectTypeIdentity identity = identities.saveAndFlush(new CrossConnectTypeIdentity());
		CrossConnectTypeHistory created = history.saveAndFlush(new CrossConnectTypeHistory(
				identity,
				name,
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getCrossConnectTypeId(),
				List.of(new AssetHistoryLink(
						created.getCrossConnectTypeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		CrossConnectTypeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "crossConnectTypeName");
		ChargeTypeIdentity chargeType = body.has("chargeTypeId")
				? optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"))
				: prior.getChargeTypeIdentity();
		prior.close(command.validOn());
		CrossConnectTypeHistory created = history.saveAndFlush(new CrossConnectTypeHistory(
				prior.getCrossConnectTypeIdentity(),
				name,
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		CrossConnectTypeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		CrossConnectTypeHistory created = history.saveAndFlush(new CrossConnectTypeHistory(
				prior.getCrossConnectTypeIdentity(),
				prior.getCrossConnectTypeName(),
				prior.getChargeTypeIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private ChargeTypeIdentity optionalChargeType(Long chargeTypeId) {
		if (chargeTypeId == null) {
			return null;
		}
		return chargeTypes.findById(chargeTypeId)
				.orElseThrow(() -> new AssetApplyException("Charge type not found: " + chargeTypeId));
	}

	private CrossConnectTypeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Cross connect type update/terminate requires assetIdentityId and baseHistoryId");
		}
		CrossConnectTypeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Cross connect type history not found: " + command.baseHistoryId()));
		if (!prior.getCrossConnectTypeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to cross connect type " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale cross connect type baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(CrossConnectTypeHistory prior, CrossConnectTypeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(
				prior.getCrossConnectTypeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(
				created.getCrossConnectTypeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getCrossConnectTypeId(), List.copyOf(links));
	}
}
