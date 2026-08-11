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
import com.dcim.workflow.AssetType;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

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
	void applyOrderRankIsTotalAndDependentsPrecedeParents(
			@ForAll @Size(min = 2, max = 8) List<AssetType> types) {
		List<AssetType> sorted = new ArrayList<>(types);
		sorted.sort(Comparator.comparingInt(AssetApplyOrder::rank).thenComparing(Enum::name));

		for (int i = 1; i < sorted.size(); i++) {
			assertThat(AssetApplyOrder.rank(sorted.get(i - 1)))
					.isLessThanOrEqualTo(AssetApplyOrder.rank(sorted.get(i)));
		}

		if (types.contains(AssetType.CAGE) && types.contains(AssetType.DATA_CENTER)) {
			assertThat(AssetApplyOrder.rank(AssetType.CAGE))
					.isLessThan(AssetApplyOrder.rank(AssetType.DATA_CENTER));
		}
		if (types.contains(AssetType.CABLE) && types.contains(AssetType.CROSS_CONNECT)) {
			assertThat(AssetApplyOrder.rank(AssetType.CABLE))
					.isLessThan(AssetApplyOrder.rank(AssetType.CROSS_CONNECT));
		}
	}

	private static final class SampleHistory extends AuditHistory {
		SampleHistory(LocalDate validFrom, Instant appliedAt) {
			super(validFrom, null, appliedAt, 1L, "ADD", "Active");
		}
	}
}
