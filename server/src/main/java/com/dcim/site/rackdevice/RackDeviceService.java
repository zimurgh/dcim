package com.dcim.site.rackdevice;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RackDeviceService {

	private final RackDeviceHistoryRepository rackDevices;

	RackDeviceService(RackDeviceHistoryRepository rackDevices) {
		this.rackDevices = rackDevices;
	}

	@Transactional(readOnly = true)
	public List<RackDeviceDto> listCurrent() {
		return rackDevices.findCurrentRackDevices().stream().map(RackDeviceDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<RackDeviceDto> listCurrentByRack(Long rackId) {
		return rackDevices.findCurrentByRackId(rackId).stream().map(RackDeviceDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<RackDeviceDto> findCurrent(Long rackDeviceId) {
		return rackDevices.findCurrentByRackDeviceId(rackDeviceId).map(RackDeviceDto::from);
	}

	@Transactional(readOnly = true)
	public List<RackDeviceDto> history(Long rackDeviceId) {
		return rackDevices.findByRackDeviceIdentity_RackDeviceIdOrderByRackDeviceHistoryIdAsc(rackDeviceId).stream()
				.map(RackDeviceDto::from)
				.toList();
	}
}
