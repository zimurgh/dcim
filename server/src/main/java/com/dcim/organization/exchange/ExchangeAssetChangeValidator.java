package com.dcim.organization.exchange;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationIssue;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ExchangeAssetChangeValidator extends AbstractAssetChangeValidator<ExchangeHistory> {

	private final ExchangeHistoryRepository history;

	ExchangeAssetChangeValidator(ExchangeHistoryRepository history, JsonPayloads payloads) {
		super(
				"EXCHANGE",
				"exchange",
				Set.of("exchangeName", "exchangeCode", "exchangeAbbreviation", "exchangeType"),
				history,
				ExchangeHistory::getExchangeId,
				payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, ExchangeHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "exchangeName", issues);
		PayloadValidation.requireText(body, "exchangeCode", issues);
		PayloadValidation.requireText(body, "exchangeAbbreviation", issues);
		validateExchangeType(body, issues);

		String exchangeName = PayloadValidation.textOrNull(body, "exchangeName");
		if (exchangeName != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(exchangeName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"exchangeName",
						"Another active exchange already uses name: " + exchangeName));
			}
		}
	}

	private static void validateExchangeType(JsonNode body, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "exchangeType", issues);
		String raw = PayloadValidation.textOrNull(body, "exchangeType");
		if (raw == null) {
			return;
		}
		try {
			ExchangeType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE,
					"exchangeType",
					"exchangeType must be one of OPTIONS, EQUITIES, FUTURES: " + raw));
		}
	}
}
