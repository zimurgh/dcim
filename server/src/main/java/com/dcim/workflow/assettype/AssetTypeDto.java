package com.dcim.workflow.assettype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record AssetTypeDto(
		Long assetTypeId,
		Long assetTypeHistoryId,
		String assetTypeCode,
		String assetTypeName,
		int applyRank,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static AssetTypeDto from(AssetTypeHistory history) {
		return new AssetTypeDto(
				history.getAssetTypeId(),
				history.getAssetTypeHistoryId(),
				history.getAssetTypeCode(),
				history.getAssetTypeName(),
				history.getApplyRank(),
				AuditSlice.from(history));
	}
}
