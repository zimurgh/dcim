package com.dcim.site.rackdevicetype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RackDeviceTypeDto(
		Long rackDeviceTypeId,
		Long rackDeviceTypeHistoryId,
		String rackDeviceTypeName,
		RackDeviceTypeKind rackDeviceTypeKind,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static RackDeviceTypeDto from(RackDeviceTypeHistory history) {
		return new RackDeviceTypeDto(
				history.getRackDeviceTypeId(),
				history.getRackDeviceTypeHistoryId(),
				history.getRackDeviceTypeName(),
				history.getRackDeviceTypeKind(),
				AuditSlice.from(history));
	}
}
