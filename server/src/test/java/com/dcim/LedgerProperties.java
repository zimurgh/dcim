package com.dcim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dcim.asset.AuditHistory;
import com.dcim.workflow.AssetApplyOrder;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Property tests for ledger invariants called out in DESIGN.md (linear history, apply order).
 */
class LedgerProperties {

	@Property
	void historyCloseIsMonotonicOnce(@ForAll @IntRange(min = 1, max = 365) int dayOffset) {
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate closeOn = from.plusDays(dayOffset);
		SampleHistory row = new SampleHistory(from, Instant.parse("2026-01-01T00:00:00Z"));
		assertThat(row.isCurrent()).isTrue();

		row.close(closeOn);
		assertThat(row.isCurrent()).isFalse();
		assertThat(row.getValidTo()).isEqualTo(closeOn);

		assertThatThrownBy(() -> row.close(closeOn.plusDays(1)))
				.isInstanceOf(IllegalStateException.class);
	}

	@Property
	void applyOrderRankIsTotalAndDependentsPrecedeParents(@ForAll("assetTypeCodeLists") List<String> types) {
		List<String> sorted = new ArrayList<>(types);
		sorted.sort(Comparator.comparingInt(AssetApplyOrder::rank).thenComparing(code -> code));

		for (int i = 1; i < sorted.size(); i++) {
			assertThat(AssetApplyOrder.rank(sorted.get(i - 1)))
					.isLessThanOrEqualTo(AssetApplyOrder.rank(sorted.get(i)));
		}

		if (types.contains("CAGE") && types.contains("DATA_CENTER")) {
			assertThat(AssetApplyOrder.rank("CAGE"))
					.isLessThan(AssetApplyOrder.rank("DATA_CENTER"));
		}
		if (types.contains("CABLE") && types.contains("CROSS_CONNECT")) {
			assertThat(AssetApplyOrder.rank("CABLE"))
					.isLessThan(AssetApplyOrder.rank("CROSS_CONNECT"));
		}
	}

	@Provide
	Arbitrary<List<String>> assetTypeCodeLists() {
		return Arbitraries.of(AssetApplyOrder.knownCodes().toArray(String[]::new))
				.list()
				.ofMinSize(2)
				.ofMaxSize(8);
	}

	private static final class SampleHistory extends AuditHistory {
		SampleHistory(LocalDate validFrom, Instant appliedAt) {
			super(validFrom, null, appliedAt, 1L, "ADD", "Active");
		}
	}
}
