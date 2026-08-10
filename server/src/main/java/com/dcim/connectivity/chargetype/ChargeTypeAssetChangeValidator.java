package com.dcim.connectivity.chargetype;

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
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeDto;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeService;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeDto;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ChargeTypeAssetChangeValidator extends AbstractAssetChangeValidator<ChargeTypeHistory> {

	private final ChargeTypeHistoryRepository history;
	private final CrossConnectTypeService crossConnectTypes;
	private final MarketDataFeedTypeService marketDataFeedTypes;

	ChargeTypeAssetChangeValidator(
			ChargeTypeHistoryRepository history,
			CrossConnectTypeService crossConnectTypes,
			MarketDataFeedTypeService marketDataFeedTypes,
			JsonPayloads payloads) {
		super(
				"CHARGE_TYPE",
				"charge type",
				Set.of("chargeTypeName"),
				history,
				ChargeTypeHistory::getChargeTypeId,
				payloads);
		this.history = history;
		this.crossConnectTypes = crossConnectTypes;
		this.marketDataFeedTypes = marketDataFeedTypes;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			ChargeTypeHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "chargeTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "chargeTypeName");
		if (name != null) {
			Long selfId = prior == null ? null : prior.getChargeTypeId();
			List<Long> clashes = history.findCurrent().stream()
					.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
					.filter(h -> h.getChargeTypeName().equalsIgnoreCase(name))
					.map(ChargeTypeHistory::getChargeTypeId)
					.filter(id -> !Objects.equals(id, selfId))
					.toList();
			if (!clashes.isEmpty()) {
				issues.add(new ValidationIssue(
						ValidationCodes.NAME_CLASH,
						"chargeTypeName",
						"chargeTypeName already used by an active charge type",
						clashes));
			}
		}
	}

	@Override
	protected void validateTerminate(
			ChargeTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		Long chargeTypeId = prior.getChargeTypeId();
		List<Long> blockingTypes = crossConnectTypes.listCurrentByChargeTypeId(chargeTypeId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectTypeDto::crossConnectTypeId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT_TYPE", id))
				.toList();
		if (!blockingTypes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Charge type is referenced by active cross connect types",
					blockingTypes));
		}
		List<Long> blockingFeedTypes = marketDataFeedTypes.listCurrentByChargeTypeId(chargeTypeId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(MarketDataFeedTypeDto::marketDataFeedTypeId)
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED_TYPE", id))
				.toList();
		if (!blockingFeedTypes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Charge type is referenced by active market data feed types",
					blockingFeedTypes));
		}
	}
}
