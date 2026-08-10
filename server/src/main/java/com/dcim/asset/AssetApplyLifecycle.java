package com.dcim.asset;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shared ADD/UPDATE/TERMINATE apply mechanics: concurrency check and history-link results.
 */
public final class AssetApplyLifecycle {

	private AssetApplyLifecycle() {
	}

	public static <H extends AuditHistory> H requireCurrentBase(
			AssetApplyCommand command,
			Function<Long, Optional<H>> historyLookup,
			Function<H, Long> identityId,
			String assetLabel) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					assetLabel + " update/terminate requires assetIdentityId and baseHistoryId");
		}
		H prior = historyLookup.apply(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						assetLabel + " history not found: " + command.baseHistoryId()));
		if (!identityId.apply(prior).equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to " + assetLabel + " " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale " + assetLabel + " baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	public static AssetApplyResult created(Long assetIdentityId, Long createdHistoryId) {
		return new AssetApplyResult(
				assetIdentityId,
				List.of(new AssetHistoryLink(createdHistoryId, AssetHistoryLink.ROLE_CREATED)));
	}

	public static AssetApplyResult revised(Long assetIdentityId, Long closedPriorHistoryId, Long createdHistoryId) {
		return new AssetApplyResult(
				assetIdentityId,
				List.of(
						new AssetHistoryLink(closedPriorHistoryId, AssetHistoryLink.ROLE_CLOSED_PRIOR),
						new AssetHistoryLink(createdHistoryId, AssetHistoryLink.ROLE_CREATED)));
	}
}
