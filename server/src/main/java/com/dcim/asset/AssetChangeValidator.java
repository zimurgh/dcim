package com.dcim.asset;

import java.util.List;

/**
 * Type-specific validation for staged changes. Domain modules implement one per asset type.
 * Validation does not mutate inventory; apply refuses when issues are present.
 */
public interface AssetChangeValidator {

	boolean supports(String assetType);

	List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context);
}
