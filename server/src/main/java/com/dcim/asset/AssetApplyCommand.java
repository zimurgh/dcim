package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Command for type-specific asset apply (organization / site implement {@link AssetChangeApplier}).
 * {@code appliedBy} is the applying user's stable id.
 */
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
