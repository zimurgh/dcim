package com.dcim.workflow;

import java.time.Instant;
import java.util.List;

public record ChangeDto(
		Long changeId,
		ChangeStage stage,
		String statusLabel,
		String body,
		String assetType,
		Long assetTypeId,
		ChangeAction action,
		Long assetIdentityId,
		Long baseHistoryId,
		Long payloadId,
		Instant createdOrStagedAt,
		String actor,
		Long appliedBy,
		String appliedByName,
		Long changeSpecId,
		List<HistoryLinkDto> historyLinks) {

	public record HistoryLinkDto(String assetType, Long assetTypeId, Long historyId, HistoryLinkRole role) {
	}

	static ChangeDto untracked(ChangeUntracked row, String statusLabel) {
		return new ChangeDto(
				row.getChangeId(),
				ChangeStage.UNTRACKED,
				statusLabel,
				row.getPayload().getBody(),
				null,
				null,
				null,
				null,
				null,
				row.getPayload().getChangePayloadId(),
				row.getCreatedAt(),
				row.getCreatedBy(),
				null,
				null,
				null,
				List.of());
	}

	static ChangeDto staged(ChangeStaged row, String assetTypeCode, String statusLabel) {
		return new ChangeDto(
				row.getChangeId(),
				ChangeStage.STAGED,
				statusLabel,
				row.getPayload().getBody(),
				assetTypeCode,
				row.getAssetType().getAssetTypeId(),
				row.getAction(),
				row.getAssetIdentityId(),
				row.getBaseHistoryId(),
				row.getPayload().getChangePayloadId(),
				row.getStagedAt(),
				row.getStagedBy(),
				null,
				null,
				null,
				List.of());
	}

	static ChangeDto committed(
			ChangeCommitted row,
			String assetTypeCode,
			String statusLabel,
			Long assetIdentityId,
			List<HistoryLinkDto> links) {
		return new ChangeDto(
				row.getChangeId(),
				ChangeStage.COMMITTED,
				statusLabel,
				row.getPayload().getBody(),
				assetTypeCode,
				row.getAssetType().getAssetTypeId(),
				row.getAction(),
				assetIdentityId,
				null,
				row.getPayload().getChangePayloadId(),
				row.getAppliedAt(),
				null,
				row.getAppliedBy(),
				null,
				null,
				links);
	}

	static ChangeDto from(ChangeView view) {
		return new ChangeDto(
				view.getChangeId(),
				view.getStage(),
				view.getStatusLabel(),
				view.getBody(),
				view.getAssetType(),
				view.getAssetTypeId(),
				view.getAction(),
				view.getAssetIdentityId(),
				view.getBaseHistoryId(),
				view.getChangePayloadId(),
				view.getEventAt(),
				view.getActor(),
				view.getAppliedBy(),
				view.getAppliedByName(),
				view.getChangeSpecId(),
				List.of());
	}
}
