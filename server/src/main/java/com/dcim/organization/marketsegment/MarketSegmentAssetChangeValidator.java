package com.dcim.organization.marketsegment;

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
class MarketSegmentAssetChangeValidator extends AbstractAssetChangeValidator<MarketSegmentHistory> {

	private final MarketSegmentHistoryRepository history;

	MarketSegmentAssetChangeValidator(MarketSegmentHistoryRepository history, JsonPayloads payloads) {
		super(
				"MARKET_SEGMENT",
				"market segment",
				Set.of("marketSegmentName", "marketSegmentType"),
				history,
				MarketSegmentHistory::getMarketSegmentId,
				payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			MarketSegmentHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "marketSegmentName", issues);
		validateMarketSegmentType(body, issues);

		String name = PayloadValidation.textOrNull(body, "marketSegmentName");
		if (name != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"marketSegmentName",
						"Another active market segment already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			MarketSegmentHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		Long marketSegmentId = prior.getMarketSegmentId();
		List<Long> blockingCrossConnects = history
				.findActiveCrossConnectIdsReferencingMarketSegment(marketSegmentId).stream()
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (blockingCrossConnects.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_REFERENCES,
				null,
				"Market segment is referenced by active cross connects",
				blockingCrossConnects.toArray(new Long[0])));
	}

	private static void validateMarketSegmentType(JsonNode body, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "marketSegmentType", issues);
		String raw = PayloadValidation.textOrNull(body, "marketSegmentType");
		if (raw == null) {
			return;
		}
		try {
			MarketSegmentType.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE,
					"marketSegmentType",
					"marketSegmentType must be Equities Index or Agricultural Futures: " + raw));
		}
	}
}
