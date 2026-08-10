package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcim.asset.ValidationIssue;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeSpecDto;
import com.dcim.workflow.ChangeSpecService;
import com.dcim.workflow.ChangeSpecStatus;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.ChangeTestSupport;
import com.dcim.workflow.ChangeValidationResult;
import com.dcim.workflow.ValidationFailedException;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared fixtures and assertions for validation-rule coverage tests.
 */
abstract class ValidationTestSupport extends ChangeTestSupport {

	@Autowired
	ChangeSpecService changeSpecs;

	// ---------------------------------------------------------------- validation assertions

	ChangeValidationResult validate(Long changeId) {
		return changes.validateStaged(changeId);
	}

	void assertValid(Long changeId) {
		ChangeValidationResult result = changes.validateStaged(changeId);
		assertThat(result.issues())
				.withFailMessage("Expected no validation issues but found: %s", result.issues())
				.isEmpty();
	}

	void assertInvalid(Long changeId, String expectedCode) {
		ChangeValidationResult result = changes.validateStaged(changeId);
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).extracting(ValidationIssue::code).contains(expectedCode);
	}

	void assertApplySucceeds(Long changeId) {
		ChangeDto applied = changes.applyStaged(changeId, appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
	}

	void assertApplyBlocked(Long changeId, String expectedCode) {
		assertThatThrownBy(() -> changes.applyStaged(changeId, appliedBy))
				.isInstanceOf(ValidationFailedException.class)
				.satisfies(ex -> {
					ValidationFailedException failed = (ValidationFailedException) ex;
					assertThat(failed.getIssues()).extracting(ValidationIssue::code).contains(expectedCode);
				});
	}

	// ---------------------------------------------------------------- Change Spec helpers

	ChangeSpecDto createSpec(Long ownerFirmId) {
		return changeSpecs.create(ownerFirmId, unique("Spec"), "tester");
	}

	void addToSpec(Long specId, Long changeId) {
		changeSpecs.addChange(specId, changeId);
	}

	ChangeSpecDto submitPendingBillingWithChrec(Long specId) {
		changeSpecs.linkChrec(specId, unique("CHREC"), "Test CHREC", "https://jira.example/CHREC");
		return changeSpecs.submitPendingBilling(specId);
	}

	void assertSpecValid(Long specId) {
		ChangeValidationResult result = changeSpecs.validate(specId);
		assertThat(result.issues())
				.withFailMessage("Expected no validation issues but found: %s", result.issues())
				.isEmpty();
	}

	void assertSpecInvalid(Long specId, String expectedCode) {
		ChangeValidationResult result = changeSpecs.validate(specId);
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).extracting(ValidationIssue::code).contains(expectedCode);
	}

	void assertSpecApplySucceeds(Long specId) {
		ChangeSpecDto applied = changeSpecs.apply(specId, appliedBy);
		assertThat(applied.status()).isEqualTo(ChangeSpecStatus.APPLIED);
	}
}
