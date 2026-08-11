package com.dcim.organization.firm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FirmAssetChangeValidatorUnsupportedActionTests extends ChangeTestSupport {

	@Autowired
	FirmAssetChangeValidator validator;

	@Test
	void unsupportedActionEmitsCode() {
		List<ValidationIssue> issues = validator.validate(
				new AssetValidateCommand("FIRM", "CLONE", "{\"firmName\":\"X\"}", null, null),
				ValidationContext.empty());
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.UNSUPPORTED_ACTION);
	}
}
