package com.dcim.site.datacenter;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record DataCenterDto(
		Long dataCenterId,
		Long dataCenterHistoryId,
		String dataCenterName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static DataCenterDto from(DataCenterHistory history) {
		return new DataCenterDto(
				history.getDataCenterId(),
				history.getDataCenterHistoryId(),
				history.getDataCenterName(),
				AuditSlice.from(history));
	}
}
