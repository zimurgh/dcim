package com.dcim.asset;

public record AssetValidateCommand(
		String assetType,
		String action,
		String payloadJson,
		Long assetIdentityId,
		Long baseHistoryId) {
}
