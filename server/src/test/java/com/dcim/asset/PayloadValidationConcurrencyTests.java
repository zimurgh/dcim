package com.dcim.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class PayloadValidationConcurrencyTests {

	private static final Function<Long, Optional<String>> MISS = id -> Optional.empty();
	private static final Function<Long, Optional<String>> HIT = id -> Optional.of("row");
	private static final Function<String, Long> OWNED_BY_10 = row -> 10L;
	private static final Function<String, Long> OWNED_BY_11 = row -> 11L;
	private static final Predicate<String> CURRENT = row -> true;
	private static final Predicate<String> CLOSED = row -> false;

	@Test
	void missingIdentityWhenEitherIdAbsent() {
		List<ValidationIssue> issues = new ArrayList<>();
		assertThat(PayloadValidation.validateConcurrency(null, 1L, MISS, OWNED_BY_10, CURRENT, issues))
				.isNull();
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.MISSING_IDENTITY);

		issues.clear();
		assertThat(PayloadValidation.validateConcurrency(1L, null, MISS, OWNED_BY_10, CURRENT, issues))
				.isNull();
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.MISSING_IDENTITY);
	}

	@Test
	void historyNotFoundWhenLookupMisses() {
		List<ValidationIssue> issues = new ArrayList<>();
		assertThat(PayloadValidation.validateConcurrency(10L, 99L, MISS, OWNED_BY_10, CURRENT, issues))
				.isNull();
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.HISTORY_NOT_FOUND);
	}

	@Test
	void identityMismatchWhenHistoryBelongsElsewhere() {
		List<ValidationIssue> issues = new ArrayList<>();
		assertThat(PayloadValidation.validateConcurrency(10L, 5L, HIT, OWNED_BY_11, CURRENT, issues))
				.isNull();
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.IDENTITY_MISMATCH);
	}

	@Test
	void staleBaseWhenHistoryAlreadyClosed() {
		List<ValidationIssue> issues = new ArrayList<>();
		assertThat(PayloadValidation.validateConcurrency(10L, 5L, HIT, OWNED_BY_10, CLOSED, issues))
				.isNull();
		assertThat(issues).extracting(ValidationIssue::code).containsExactly(ValidationCodes.STALE_BASE);
	}

	@Test
	void returnsBaseWhenCurrentAndMatching() {
		List<ValidationIssue> issues = new ArrayList<>();
		assertThat(PayloadValidation.validateConcurrency(10L, 5L, HIT, OWNED_BY_10, CURRENT, issues))
				.isEqualTo("row");
		assertThat(issues).isEmpty();
	}
}
