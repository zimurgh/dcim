package com.dcim.workflow.assettype;

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
import com.dcim.workflow.ChangeCommittedRepository;
import com.dcim.workflow.ChangeStagedRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class AssetTypeAssetChangeValidator extends AbstractAssetChangeValidator<AssetTypeHistory> {

	private final AssetTypeHistoryRepository history;
	private final ChangeStagedRepository staged;
	private final ChangeCommittedRepository committed;

	AssetTypeAssetChangeValidator(
			AssetTypeHistoryRepository history,
			ChangeStagedRepository staged,
			ChangeCommittedRepository committed,
			JsonPayloads payloads) {
		super(
				"ASSET_TYPE",
				"asset type",
				Set.of("assetTypeCode", "assetTypeName", "applyRank"),
				history,
				AssetTypeHistory::getAssetTypeId,
				payloads);
		this.history = history;
		this.staged = staged;
		this.committed = committed;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			AssetTypeHistory prior,
			List<ValidationIssue> issues) {
		if (prior == null) {
			PayloadValidation.requireText(body, "assetTypeCode", issues);
			String code = PayloadValidation.textOrNull(body, "assetTypeCode");
			if (code != null) {
				String normalized = code.toUpperCase();
				List<Long> clashes = history.findCurrent().stream()
						.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
						.filter(h -> h.getAssetTypeCode().equalsIgnoreCase(normalized))
						.map(AssetTypeHistory::getAssetTypeId)
						.toList();
				if (!clashes.isEmpty()) {
					issues.add(new ValidationIssue(
							ValidationCodes.NAME_CLASH,
							"assetTypeCode",
							"assetTypeCode already used by an active asset type",
							clashes));
				}
			}
			JsonNode rank = body.get("applyRank");
			if (rank == null || rank.isNull() || !rank.canConvertToLong()) {
				issues.add(new ValidationIssue(
						ValidationCodes.MISSING_FIELD,
						"applyRank",
						"applyRank is required",
						List.of()));
			}
		}
		PayloadValidation.requireText(body, "assetTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "assetTypeName");
		if (name != null) {
			Long selfId = prior == null ? null : prior.getAssetTypeId();
			List<Long> clashes = history.findCurrent().stream()
					.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
					.filter(h -> h.getAssetTypeName().equalsIgnoreCase(name))
					.map(AssetTypeHistory::getAssetTypeId)
					.filter(id -> !Objects.equals(id, selfId))
					.toList();
			if (!clashes.isEmpty()) {
				issues.add(new ValidationIssue(
						ValidationCodes.NAME_CLASH,
						"assetTypeName",
						"assetTypeName already used by an active asset type",
						clashes));
			}
		}
	}

	@Override
	protected void validateTerminate(
			AssetTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		AssetTypeIdentity identity = prior.getAssetTypeIdentity();
		if (staged.existsByAssetType(identity) || committed.existsByAssetType(identity)) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Asset type is referenced by staged or committed changes",
					List.of(prior.getAssetTypeId())));
		}
	}
}
