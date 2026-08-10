package com.dcim.site.rackdevicetype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RackDeviceTypeService {

	private final RackDeviceTypeHistoryRepository types;

	RackDeviceTypeService(RackDeviceTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<RackDeviceTypeDto> listCurrent() {
		return types.findCurrent().stream().map(RackDeviceTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<RackDeviceTypeDto> findCurrent(Long rackDeviceTypeId) {
		return types.findCurrentByRackDeviceTypeId(rackDeviceTypeId).map(RackDeviceTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<RackDeviceTypeDto> history(Long rackDeviceTypeId) {
		return types.findByRackDeviceTypeIdentity_RackDeviceTypeIdOrderByRackDeviceTypeHistoryIdAsc(rackDeviceTypeId)
				.stream()
				.map(RackDeviceTypeDto::from)
				.toList();
	}
}
