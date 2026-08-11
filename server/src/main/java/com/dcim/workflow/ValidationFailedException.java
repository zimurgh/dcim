package com.dcim.workflow;

import java.util.List;

import com.dcim.asset.ValidationIssue;

public class ValidationFailedException extends WorkflowException {

	private final List<ValidationIssue> issues;

	public ValidationFailedException(List<ValidationIssue> issues) {
		super(summarize(issues));
		this.issues = List.copyOf(issues);
	}

	public List<ValidationIssue> getIssues() {
		return issues;
	}

	private static String summarize(List<ValidationIssue> issues) {
		if (issues == null || issues.isEmpty()) {
			return "Validation failed";
		}
		if (issues.size() == 1) {
			return issues.getFirst().message();
		}
		return "Validation failed with " + issues.size() + " issues";
	}
}
