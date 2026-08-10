package com.dcim.organization.exchange;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ExchangeAssetChangeApplier implements AssetChangeApplier {

	private final ExchangeIdentityRepository identities;
	private final ExchangeHistoryRepository history;
	private final JsonPayloads payloads;

	ExchangeAssetChangeApplier(
			ExchangeIdentityRepository identities,
			ExchangeHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "EXCHANGE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported exchange action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "exchangeName");
		String code = JsonPayloads.requiredText(body, "exchangeCode");
		String abbreviation = JsonPayloads.requiredText(body, "exchangeAbbreviation");
		ExchangeType type = requireExchangeType(body);
		ExchangeIdentity identity = identities.saveAndFlush(new ExchangeIdentity());
		ExchangeHistory created = history.saveAndFlush(new ExchangeHistory(
				identity,
				name,
				code,
				abbreviation,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getExchangeId(),
				List.of(new AssetHistoryLink(created.getExchangeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		ExchangeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "exchangeName");
		String code = JsonPayloads.requiredText(body, "exchangeCode");
		String abbreviation = JsonPayloads.requiredText(body, "exchangeAbbreviation");
		ExchangeType type = requireExchangeType(body);
		prior.close(command.validOn());
		ExchangeHistory created = history.saveAndFlush(new ExchangeHistory(
				prior.getExchangeIdentity(),
				name,
				code,
				abbreviation,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		ExchangeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		ExchangeHistory created = history.saveAndFlush(new ExchangeHistory(
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
				command.committedStatus()));
		return result(prior, created);
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

	private ExchangeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Exchange update/terminate requires assetIdentityId and baseHistoryId");
		}
		ExchangeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Exchange history not found: " + command.baseHistoryId()));
		if (!prior.getExchangeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to exchange " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale exchange baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(ExchangeHistory prior, ExchangeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getExchangeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getExchangeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getExchangeId(), List.copyOf(links));
	}
}
