package com.dcim.site.cage;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.datacenter.DataCenterIdentity;
import com.dcim.site.datacenter.DataCenterIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CageAssetChangeApplier extends AbstractAssetChangeApplier<CageIdentity, CageHistory> {

	private final DataCenterIdentityRepository dataCenters;

	CageAssetChangeApplier(
			CageIdentityRepository identities,
			CageHistoryRepository history,
			DataCenterIdentityRepository dataCenters,
			JsonPayloads payloads) {
		super(
				"CAGE",
				"cage",
				identities,
				history,
				payloads,
				CageIdentity::new,
				CageIdentity::getCageId,
				CageHistory::getCageId,
				CageHistory::getCageHistoryId);
		this.dataCenters = dataCenters;
	}

	@Override
	protected CageHistory createAdd(CageIdentity identity, JsonNode body, AssetApplyCommand command) {
		DataCenterIdentity dataCenter = requireDataCenter(JsonPayloads.requiredLong(body, "dataCenterId"), "add");
		return new CageHistory(
				identity,
				dataCenter,
				JsonPayloads.requiredText(body, "cageName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CageHistory createUpdate(CageHistory prior, JsonNode body, AssetApplyCommand command) {
		DataCenterIdentity dataCenter = body.hasNonNull("dataCenterId")
				? requireDataCenter(JsonPayloads.requiredLong(body, "dataCenterId"), "update")
				: prior.getDataCenterIdentity();
		return new CageHistory(
				prior.getCageIdentity(),
				dataCenter,
				JsonPayloads.requiredText(body, "cageName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CageHistory createTerminate(CageHistory prior, AssetApplyCommand command) {
		return new CageHistory(
				prior.getCageIdentity(),
				prior.getDataCenterIdentity(),
				prior.getCageName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private DataCenterIdentity requireDataCenter(Long dataCenterId, String action) {
		return dataCenters.findById(dataCenterId)
				.orElseThrow(() -> new AssetApplyException("Data center not found for cage " + action));
	}
}
