package com.dcim.site.datacenter;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

@Component
class DataCenterAssetChangeApplier implements AssetChangeApplier {

	private final DataCenterIdentityRepository identities;
	private final DataCenterHistoryRepository history;
	private final JsonPayloads payloads;

	DataCenterAssetChangeApplier(
			DataCenterIdentityRepository identities,
			DataCenterHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "DATA_CENTER".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported data center action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		String name = JsonPayloads.requiredText(payloads.read(command.payloadJson()), "dataCenterName");
		DataCenterIdentity identity = identities.saveAndFlush(new DataCenterIdentity());
		DataCenterHistory created = history.saveAndFlush(new DataCenterHistory(
				identity,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getDataCenterId(),
				List.of(new AssetHistoryLink(created.getDataCenterHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		DataCenterHistory prior = requireCurrentBase(command);
		String name = JsonPayloads.requiredText(payloads.read(command.payloadJson()), "dataCenterName");
		prior.close(command.validOn());
		DataCenterHistory created = history.saveAndFlush(new DataCenterHistory(
				prior.getDataCenterIdentity(),
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
		DataCenterHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		DataCenterHistory created = history.saveAndFlush(new DataCenterHistory(
				prior.getDataCenterIdentity(),
				prior.getDataCenterName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private DataCenterHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Data center update/terminate requires assetIdentityId and baseHistoryId");
		}
		DataCenterHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Data center history not found: " + command.baseHistoryId()));
		if (!prior.getDataCenterId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to data center " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale data center baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(DataCenterHistory prior, DataCenterHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getDataCenterHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getDataCenterHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getDataCenterId(), List.copyOf(links));
	}
}
