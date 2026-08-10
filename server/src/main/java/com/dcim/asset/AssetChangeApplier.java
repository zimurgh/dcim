package com.dcim.asset;

/**
 * Type-specific validate/apply for asset history. Implemented by organization and site modules.
 */
public interface AssetChangeApplier {

	boolean supports(String assetType);

	AssetApplyResult apply(AssetApplyCommand command);
}
