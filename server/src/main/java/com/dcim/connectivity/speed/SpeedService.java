package com.dcim.connectivity.speed;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpeedService {

	private final SpeedHistoryRepository speeds;

	SpeedService(SpeedHistoryRepository speeds) {
		this.speeds = speeds;
	}

	@Transactional(readOnly = true)
	public List<SpeedDto> listCurrent() {
		return speeds.findCurrent().stream().map(SpeedDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<SpeedDto> findCurrent(Long speedId) {
		return speeds.findCurrentBySpeedId(speedId).map(SpeedDto::from);
	}

	@Transactional(readOnly = true)
	public List<SpeedDto> history(Long speedId) {
		return speeds.findBySpeedIdentity_SpeedIdOrderBySpeedHistoryIdAsc(speedId).stream()
				.map(SpeedDto::from)
				.toList();
	}
}
