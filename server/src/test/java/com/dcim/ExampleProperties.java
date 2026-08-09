package com.dcim;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleProperties {

	@Property
	void historyIdsArePositive(@ForAll @IntRange(min = 1, max = 10_000) int historyId) {
		assertThat(historyId).isPositive();
	}
}
