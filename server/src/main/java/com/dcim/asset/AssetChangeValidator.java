package com.dcim.asset;

import java.util.List;

public interface AssetChangeValidator {

	boolean supports(String assetType);

	List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context);
}
