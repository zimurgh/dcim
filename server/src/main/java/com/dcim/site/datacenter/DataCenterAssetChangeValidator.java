package com.dcim.site.datacenter;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class DataCenterAssetChangeValidator extends AbstractAssetChangeValidator<DataCenterHistory> {

	private final DataCenterHistoryRepository history;

	DataCenterAssetChangeValidator(DataCenterHistoryRepository history, JsonPayloads payloads) {
		super(
				"DATA_CENTER",
				"data center",
				Set.of("dataCenterName"),
				history,
				DataCenterHistory::getDataCenterId,
				payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, DataCenterHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "dataCenterName", issues);
		String name = PayloadValidation.textOrNull(body, "dataCenterName");
		if (name != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"dataCenterName",
						"Another active data center already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			DataCenterHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingCages = history.findActiveCageIdsForDataCenter(prior.getDataCenterId()).stream()
				.filter(id -> !context.coversTerminate("CAGE", id))
				.toList();
		if (blockingCages.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Data center has active cages",
				blockingCages.toArray(new Long[0])));
	}
}
