package com.dcim.site.datacenter;

import java.time.Instant;
import java.time.LocalDate;

public record DataCenterDto(
		Long dataCenterId,
		Long dataCenterHistoryId,
		String dataCenterName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		String appliedBy,
		String action,
		String status) {

	static DataCenterDto from(DataCenterHistory history) {
		return new DataCenterDto(
				history.getDataCenterId(),
				history.getDataCenterHistoryId(),
				history.getDataCenterName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
