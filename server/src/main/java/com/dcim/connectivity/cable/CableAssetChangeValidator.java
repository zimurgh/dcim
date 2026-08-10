package com.dcim.connectivity.cable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.dcim.asset.AssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.AuditHistory;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;
import com.dcim.site.rackdeviceport.RackDevicePortDto;
import com.dcim.site.rackdeviceport.RackDevicePortService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CableAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("cableName", "portAId", "portBId", "crossConnectId");

	private final CableHistoryRepository history;
	private final RackDevicePortService ports;
	private final CrossConnectService crossConnects;
	private final JsonPayloads payloads;

	CableAssetChangeValidator(
			CableHistoryRepository history,
			RackDevicePortService ports,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		this.history = history;
		this.ports = ports;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CABLE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION, "Unsupported cable action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		CableHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					CableHistory::getCableId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			return issues;
		}

		PayloadValidation.requireText(body, "cableName", issues);

		Long priorPortAId = base == null ? null : base.getPortAId();
		Long priorPortBId = base == null ? null : base.getPortBId();
		Long portAId = PayloadValidation.resolveRequiredReference(
				body, "portAId", isAdd, priorPortAId, issues,
				id -> ports.findCurrent(id).map(RackDevicePortDto::status).orElse(null));
		Long portBId = PayloadValidation.resolveRequiredReference(
				body, "portBId", isAdd, priorPortBId, issues,
				id -> ports.findCurrent(id).map(RackDevicePortDto::status).orElse(null));

		PayloadValidation.validateOptionalReference(
				body, "crossConnectId", issues,
				id -> crossConnects.findCurrent(id).map(CrossConnectDto::status).orElse(null));

		Long selfId = command.assetIdentityId();
		if (portAId != null && portBId != null) {
			if (portAId.equals(portBId)) {
				issues.add(new ValidationIssue(
						ValidationCodes.VALUE_CLASH, "portBId", "Cable ports must be distinct"));
			}
			else {
				validatePortClash(portAId, "portAId", selfId, issues);
				validatePortClash(portBId, "portBId", selfId, issues);
			}
		}

		return issues;
	}

	private JsonNode readBody(AssetValidateCommand command, List<ValidationIssue> issues) {
		try {
			return payloads.read(command.payloadJson());
		}
		catch (RuntimeException ex) {
			issues.add(new ValidationIssue(ValidationCodes.INVALID_PAYLOAD, ex.getMessage()));
			return null;
		}
	}

	private void validatePortClash(Long portId, String field, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrentByPortId(portId).stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.map(CableHistory::getCableId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.VALUE_CLASH, field, "Port already used by an active cable", clashes));
		}
	}
}
