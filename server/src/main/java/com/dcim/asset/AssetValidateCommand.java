package com.dcim.asset;

/**
 * Intent to validate (mirrors staged change; no apply timestamps).
 */
public record AssetValidateCommand(
		String assetType,
		String action,
		String payloadJson,
		Long assetIdentityId,
		Long baseHistoryId) {
}
