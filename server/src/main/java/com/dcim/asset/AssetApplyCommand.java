package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Command for type-specific asset apply (organization / site implement {@link AssetChangeApplier}).
 */
public record AssetApplyCommand(
		String assetType,
		String action,
		String payloadJson,
		Long assetIdentityId,
		Long baseHistoryId,
		Instant appliedAt,
		LocalDate validOn,
		String appliedBy,
		String committedStatus) {
}
