package com.dcim.site.rackdeviceport;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RackDevicePortService {

	private final RackDevicePortHistoryRepository ports;

	RackDevicePortService(RackDevicePortHistoryRepository ports) {
		this.ports = ports;
	}

	@Transactional(readOnly = true)
	public List<RackDevicePortDto> listCurrent() {
		return ports.findCurrentRackDevicePorts().stream().map(RackDevicePortDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<RackDevicePortDto> listCurrentByRackDevice(Long rackDeviceId) {
		return ports.findCurrentByRackDeviceId(rackDeviceId).stream().map(RackDevicePortDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<RackDevicePortDto> findCurrent(Long rackDevicePortId) {
		return ports.findCurrentByRackDevicePortId(rackDevicePortId).map(RackDevicePortDto::from);
	}

	@Transactional(readOnly = true)
	public List<RackDevicePortDto> history(Long rackDevicePortId) {
		return ports.findByRackDevicePortIdentity_RackDevicePortIdOrderByRackDevicePortHistoryIdAsc(rackDevicePortId)
				.stream()
				.map(RackDevicePortDto::from)
				.toList();
	}
}
