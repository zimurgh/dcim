package com.dcim.connectivity.chargetype;

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
class ChargeTypeAssetChangeApplier implements AssetChangeApplier {

	private final ChargeTypeIdentityRepository identities;
	private final ChargeTypeHistoryRepository history;
	private final JsonPayloads payloads;

	ChargeTypeAssetChangeApplier(
			ChargeTypeIdentityRepository identities,
			ChargeTypeHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CHARGE_TYPE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported charge type action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "chargeTypeName");
		ChargeTypeIdentity identity = identities.saveAndFlush(new ChargeTypeIdentity());
		ChargeTypeHistory created = history.saveAndFlush(new ChargeTypeHistory(
				identity,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getChargeTypeId(),
				List.of(new AssetHistoryLink(created.getChargeTypeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		ChargeTypeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "chargeTypeName");
		prior.close(command.validOn());
		ChargeTypeHistory created = history.saveAndFlush(new ChargeTypeHistory(
				prior.getChargeTypeIdentity(),
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
		ChargeTypeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		ChargeTypeHistory created = history.saveAndFlush(new ChargeTypeHistory(
				prior.getChargeTypeIdentity(),
				prior.getChargeTypeName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private ChargeTypeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Charge type update/terminate requires assetIdentityId and baseHistoryId");
		}
		ChargeTypeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Charge type history not found: " + command.baseHistoryId()));
		if (!prior.getChargeTypeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to charge type " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale charge type baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(ChargeTypeHistory prior, ChargeTypeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getChargeTypeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getChargeTypeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getChargeTypeId(), List.copyOf(links));
	}
}
