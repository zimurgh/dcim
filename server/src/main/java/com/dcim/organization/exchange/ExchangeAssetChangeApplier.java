package com.dcim.organization.exchange;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ExchangeAssetChangeApplier extends AbstractAssetChangeApplier<ExchangeIdentity, ExchangeHistory> {

	ExchangeAssetChangeApplier(
			ExchangeIdentityRepository identities,
			ExchangeHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"EXCHANGE",
				"exchange",
				identities,
				history,
				payloads,
				ExchangeIdentity::new,
				ExchangeIdentity::getExchangeId,
				ExchangeHistory::getExchangeId,
				ExchangeHistory::getExchangeHistoryId);
	}

	@Override
	protected ExchangeHistory createAdd(ExchangeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new ExchangeHistory(
				identity,
				JsonPayloads.requiredText(body, "exchangeName"),
				JsonPayloads.requiredText(body, "exchangeCode"),
				JsonPayloads.requiredText(body, "exchangeAbbreviation"),
				requireExchangeType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected ExchangeHistory createUpdate(ExchangeHistory prior, JsonNode body, AssetApplyCommand command) {
		return new ExchangeHistory(
				prior.getExchangeIdentity(),
				JsonPayloads.requiredText(body, "exchangeName"),
				JsonPayloads.requiredText(body, "exchangeCode"),
				JsonPayloads.requiredText(body, "exchangeAbbreviation"),
				requireExchangeType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected ExchangeHistory createTerminate(ExchangeHistory prior, AssetApplyCommand command) {
		return new ExchangeHistory(
				prior.getExchangeIdentity(),
				prior.getExchangeName(),
				prior.getExchangeCode(),
				prior.getExchangeAbbreviation(),
				prior.getExchangeType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private static ExchangeType requireExchangeType(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "exchangeType");
		try {
			return ExchangeType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException(
					"exchangeType must be one of OPTIONS, EQUITIES, FUTURES: " + raw);
		}
	}
}
