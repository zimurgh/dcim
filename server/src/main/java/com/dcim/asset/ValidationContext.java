package com.dcim.asset;

import java.util.List;
import java.util.Objects;

/**
 * Batch-aware context for validators: other staged intents that will apply together.
 */
public final class ValidationContext {

	private final List<BatchIntent> batch;

	public ValidationContext(List<BatchIntent> batch) {
		this.batch = List.copyOf(batch == null ? List.of() : batch);
	}

	public static ValidationContext empty() {
		return new ValidationContext(List.of());
	}

	public List<BatchIntent> batch() {
		return batch;
	}

	public boolean coversTerminate(String assetType, Long assetIdentityId) {
		if (assetIdentityId == null) {
			return false;
		}
		return batch.stream().anyMatch(intent -> intent.coversTerminate(assetType, assetIdentityId));
	}

	/**
	 * Another staged change in the same apply batch.
	 */
	public record BatchIntent(String assetType, String action, Long assetIdentityId) {

		public boolean coversTerminate(String assetType, Long assetIdentityId) {
			return "TERMINATE".equals(action)
					&& Objects.equals(this.assetType, assetType)
					&& Objects.equals(this.assetIdentityId, assetIdentityId);
		}
	}
}
