package com.dcim.site.rackdeviceporttype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RackDevicePortTypeDto(
		Long rackDevicePortTypeId,
		Long rackDevicePortTypeHistoryId,
		String rackDevicePortTypeName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static RackDevicePortTypeDto from(RackDevicePortTypeHistory history) {
		return new RackDevicePortTypeDto(
				history.getRackDevicePortTypeId(),
				history.getRackDevicePortTypeHistoryId(),
				history.getRackDevicePortTypeName(),
				AuditSlice.from(history));
	}
}
