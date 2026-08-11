package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

public record AssetApplyCommand(
		String assetType,
		String action,
		String payloadJson,
		Long assetIdentityId,
		Long baseHistoryId,
		Instant appliedAt,
		LocalDate validOn,
		Long appliedBy,
		String committedStatus) {
}
