package com.dcim.site.rackdeviceport;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RackDevicePortDto(
		Long rackDevicePortId,
		Long rackDevicePortHistoryId,
		Long rackDeviceId,
		Long rackDevicePortTypeId,
		String rackDevicePortName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static RackDevicePortDto from(RackDevicePortHistory history) {
		return new RackDevicePortDto(
				history.getRackDevicePortId(),
				history.getRackDevicePortHistoryId(),
				history.getRackDeviceId(),
				history.getRackDevicePortTypeId(),
				history.getRackDevicePortName(),
				AuditSlice.from(history));
	}
}
