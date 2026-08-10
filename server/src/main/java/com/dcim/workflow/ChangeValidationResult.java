package com.dcim.workflow;

import java.util.List;

import com.dcim.asset.ValidationIssue;

public record ChangeValidationResult(Long changeId, List<ValidationIssue> issues) {

	public boolean ok() {
		return issues == null || issues.isEmpty();
	}
}
