package com.dcim.site.cage;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.site.datacenter.DataCenterIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CageAssetChangeValidator extends AbstractAssetChangeValidator<CageHistory> {

	private final CageHistoryRepository history;
	private final DataCenterIdentityRepository dataCenters;

	CageAssetChangeValidator(
			CageHistoryRepository history, DataCenterIdentityRepository dataCenters, JsonPayloads payloads) {
		super("CAGE", "cage", Set.of("cageName", "dataCenterId"), history, CageHistory::getCageId, payloads);
		this.history = history;
		this.dataCenters = dataCenters;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, CageHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "cageName", issues);
		String name = PayloadValidation.textOrNull(body, "cageName");

		boolean isAdd = prior == null;
		Long dataCenterId = null;
		if (isAdd || body.hasNonNull("dataCenterId")) {
			dataCenterId = PayloadValidation.requiredLong(body, "dataCenterId", issues);
			if (dataCenterId != null) {
				validateDataCenterReference(dataCenterId, issues);
			}
		}
		else {
			dataCenterId = prior.getDataCenterId();
		}

		if (name != null && dataCenterId != null) {
			Long excludeId = isAdd ? null : command.assetIdentityId();
			if (history.existsActiveNameClashInDataCenter(name, dataCenterId, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"cageName",
						"Another active cage in this data center already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(CageHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingRacks = history.findActiveRackIdsForCage(prior.getCageId()).stream()
				.filter(id -> !context.coversTerminate("RACK", id))
				.toList();
		if (blockingRacks.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN, null, "Cage has active racks", blockingRacks.toArray(new Long[0])));
	}

	private void validateDataCenterReference(Long dataCenterId, List<ValidationIssue> issues) {
		if (!dataCenters.existsById(dataCenterId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND,
					"dataCenterId",
					"Data center not found: " + dataCenterId));
		}
		else if (history.countActiveDataCenters(dataCenterId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE,
					"dataCenterId",
					"Data center is not currently active: " + dataCenterId));
		}
	}
}
