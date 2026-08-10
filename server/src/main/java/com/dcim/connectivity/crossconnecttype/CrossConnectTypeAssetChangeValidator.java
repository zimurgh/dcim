package com.dcim.connectivity.crossconnecttype;

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
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectTypeAssetChangeValidator extends AbstractAssetChangeValidator<CrossConnectTypeHistory> {

	private final CrossConnectTypeHistoryRepository history;
	private final ChargeTypeService chargeTypes;
	private final CrossConnectService crossConnects;

	CrossConnectTypeAssetChangeValidator(
			CrossConnectTypeHistoryRepository history,
			ChargeTypeService chargeTypes,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		super(
				"CROSS_CONNECT_TYPE",
				"cross connect type",
				Set.of("crossConnectTypeName", "chargeTypeId"),
				history,
				CrossConnectTypeHistory::getCrossConnectTypeId,
				payloads);
		this.history = history;
		this.chargeTypes = chargeTypes;
		this.crossConnects = crossConnects;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			CrossConnectTypeHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "crossConnectTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "crossConnectTypeName");
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
			CrossConnectTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blocking = crossConnects.listCurrentByCrossConnectTypeId(prior.getCrossConnectTypeId()).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectDto::crossConnectId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (!blocking.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Cross connect type is referenced by active cross connects",
					blocking));
		}
	}

	private void validateNameClash(String name, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getCrossConnectTypeName().equalsIgnoreCase(name))
				.map(CrossConnectTypeHistory::getCrossConnectTypeId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"crossConnectTypeName",
					"crossConnectTypeName already used by an active cross connect type",
					clashes));
		}
	}
}
