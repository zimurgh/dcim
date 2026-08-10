package com.dcim.site.datacenter;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class DataCenterAssetChangeApplier extends AbstractAssetChangeApplier<DataCenterIdentity, DataCenterHistory> {

	DataCenterAssetChangeApplier(
			DataCenterIdentityRepository identities,
			DataCenterHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"DATA_CENTER",
				"data center",
				identities,
				history,
				payloads,
				DataCenterIdentity::new,
				DataCenterIdentity::getDataCenterId,
				DataCenterHistory::getDataCenterId,
				DataCenterHistory::getDataCenterHistoryId);
	}

	@Override
	protected DataCenterHistory createAdd(DataCenterIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new DataCenterHistory(
				identity,
				JsonPayloads.requiredText(body, "dataCenterName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected DataCenterHistory createUpdate(DataCenterHistory prior, JsonNode body, AssetApplyCommand command) {
		return new DataCenterHistory(
				prior.getDataCenterIdentity(),
				JsonPayloads.requiredText(body, "dataCenterName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected DataCenterHistory createTerminate(DataCenterHistory prior, AssetApplyCommand command) {
		return new DataCenterHistory(
				prior.getDataCenterIdentity(),
				prior.getDataCenterName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}
}
