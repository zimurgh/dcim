package com.dcim.asset;

public interface AssetChangeApplier {

	boolean supports(String assetType);

	AssetApplyResult apply(AssetApplyCommand command);
}
