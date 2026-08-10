package com.dcim.site.cage;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.datacenter.DataCenterIdentity;
import com.dcim.site.datacenter.DataCenterIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CageAssetChangeApplier implements AssetChangeApplier {

	private final CageIdentityRepository identities;
	private final CageHistoryRepository history;
	private final DataCenterIdentityRepository dataCenters;
	private final JsonPayloads payloads;

	CageAssetChangeApplier(
			CageIdentityRepository identities,
			CageHistoryRepository history,
			DataCenterIdentityRepository dataCenters,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.dataCenters = dataCenters;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CAGE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported cage action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "cageName");
		DataCenterIdentity dataCenter = dataCenters.findById(JsonPayloads.requiredLong(body, "dataCenterId"))
				.orElseThrow(() -> new AssetApplyException("Data center not found for cage add"));
		CageIdentity identity = identities.saveAndFlush(new CageIdentity());
		CageHistory created = history.saveAndFlush(new CageHistory(
				identity,
				dataCenter,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getCageId(),
				List.of(new AssetHistoryLink(created.getCageHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		CageHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "cageName");
		DataCenterIdentity dataCenter = body.hasNonNull("dataCenterId")
				? dataCenters.findById(JsonPayloads.requiredLong(body, "dataCenterId"))
						.orElseThrow(() -> new AssetApplyException("Data center not found for cage update"))
				: prior.getDataCenterIdentity();
		prior.close(command.validOn());
		CageHistory created = history.saveAndFlush(new CageHistory(
				prior.getCageIdentity(),
				dataCenter,
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
		CageHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		CageHistory created = history.saveAndFlush(new CageHistory(
				prior.getCageIdentity(),
				prior.getDataCenterIdentity(),
				prior.getCageName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private CageHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Cage update/terminate requires assetIdentityId and baseHistoryId");
		}
		CageHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Cage history not found: " + command.baseHistoryId()));
		if (!prior.getCageId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to cage " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale cage baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(CageHistory prior, CageHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getCageHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getCageHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getCageId(), List.copyOf(links));
	}
}
