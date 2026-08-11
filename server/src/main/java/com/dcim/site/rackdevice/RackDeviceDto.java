package com.dcim.site.rackdevice;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RackDeviceDto(
		Long rackDeviceId,
		Long rackDeviceHistoryId,
		Long rackId,
		Long rackDeviceTypeId,
		String rackDeviceName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static RackDeviceDto from(RackDeviceHistory history) {
		return new RackDeviceDto(
				history.getRackDeviceId(),
				history.getRackDeviceHistoryId(),
				history.getRackId(),
				history.getRackDeviceTypeId(),
				history.getRackDeviceName(),
				AuditSlice.from(history));
	}
}
