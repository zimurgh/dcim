package com.dcim.site.rackdeviceporttype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RackDevicePortTypeService {

	private final RackDevicePortTypeHistoryRepository types;

	RackDevicePortTypeService(RackDevicePortTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<RackDevicePortTypeDto> listCurrent() {
		return types.findCurrent().stream().map(RackDevicePortTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<RackDevicePortTypeDto> findCurrent(Long rackDevicePortTypeId) {
		return types.findCurrentByRackDevicePortTypeId(rackDevicePortTypeId).map(RackDevicePortTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<RackDevicePortTypeDto> history(Long rackDevicePortTypeId) {
		return types
				.findByRackDevicePortTypeIdentity_RackDevicePortTypeIdOrderByRackDevicePortTypeHistoryIdAsc(
						rackDevicePortTypeId)
				.stream()
				.map(RackDevicePortTypeDto::from)
				.toList();
	}
}
