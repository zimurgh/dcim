package com.dcim.connectivity.marketdatafeedtype;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.chargetype.ChargeTypeDto;
import com.dcim.connectivity.chargetype.ChargeTypeService;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedDto;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketDataFeedTypeAssetChangeValidator extends AbstractAssetChangeValidator<MarketDataFeedTypeHistory> {

	private final MarketDataFeedTypeHistoryRepository history;
	private final ChargeTypeService chargeTypes;
	private final MarketDataFeedService marketDataFeeds;

	MarketDataFeedTypeAssetChangeValidator(
			MarketDataFeedTypeHistoryRepository history,
			ChargeTypeService chargeTypes,
			MarketDataFeedService marketDataFeeds,
			JsonPayloads payloads) {
		super(
				"MARKET_DATA_FEED_TYPE",
				"market data feed type",
				Set.of("marketDataFeedTypeName", "chargeTypeId"),
				history,
				MarketDataFeedTypeHistory::getMarketDataFeedTypeId,
				payloads);
		this.history = history;
		this.chargeTypes = chargeTypes;
		this.marketDataFeeds = marketDataFeeds;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			MarketDataFeedTypeHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "marketDataFeedTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "marketDataFeedTypeName");
		if (name != null) {
			validateNameClash(name, command.assetIdentityId(), issues);
		}

		if (body.has("chargeTypeId")) {
			Long chargeTypeId = PayloadValidation.longOrNull(body, "chargeTypeId", issues);
			if (chargeTypeId != null) {
				String status = chargeTypes.findCurrent(chargeTypeId).map(ChargeTypeDto::status).orElse(null);
				PayloadValidation.requireActiveReference("chargeTypeId", chargeTypeId, status, issues);
			}
		}
	}

	@Override
	protected void validateTerminate(
			MarketDataFeedTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blocking = marketDataFeeds
				.listCurrentByMarketDataFeedTypeId(prior.getMarketDataFeedTypeId()).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(MarketDataFeedDto::marketDataFeedId)
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED", id))
				.toList();
		if (!blocking.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_CHILDREN,
					null,
					"Market data feed type has active market data feeds",
					blocking));
		}
	}

	private void validateNameClash(String name, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getMarketDataFeedTypeName().equalsIgnoreCase(name))
				.map(MarketDataFeedTypeHistory::getMarketDataFeedTypeId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"marketDataFeedTypeName",
					"marketDataFeedTypeName already used by an active market data feed type",
					clashes));
		}
	}
}
