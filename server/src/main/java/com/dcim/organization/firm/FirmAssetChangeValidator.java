package com.dcim.organization.firm;

import java.util.ArrayList;
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
class FirmAssetChangeValidator extends AbstractAssetChangeValidator<FirmHistory> {

	private final FirmHistoryRepository history;

	FirmAssetChangeValidator(FirmHistoryRepository history, JsonPayloads payloads) {
		super("FIRM", "firm", Set.of("firmName", "parentFirmName"), history, FirmHistory::getFirmId, payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, FirmHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "firmName", issues);
		String firmName = PayloadValidation.textOrNull(body, "firmName");
		if (firmName != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(firmName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"firmName",
						"Another active firm already uses name: " + firmName));
			}
		}
	}

	@Override
	protected void validateTerminate(FirmHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		Long firmId = prior.getFirmId();
		List<Long> blockingCrossConnects = history.findActiveCrossConnectIdsReferencingFirm(firmId).stream()
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		List<Long> blockingFeeds = history.findActiveMarketDataFeedIdsReferencingFirm(firmId).stream()
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED", id))
				.toList();
		if (blockingCrossConnects.isEmpty() && blockingFeeds.isEmpty()) {
			return;
		}
		List<Long> related = new ArrayList<>();
		related.addAll(blockingCrossConnects);
		related.addAll(blockingFeeds);
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_REFERENCES,
				null,
				"Firm is referenced by active cross connects or market data feeds",
				related.toArray(new Long[0])));
	}
}
