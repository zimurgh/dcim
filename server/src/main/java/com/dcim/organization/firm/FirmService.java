package com.dcim.organization.firm;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirmService {

	private final FirmHistoryRepository firms;

	FirmService(FirmHistoryRepository firms) {
		this.firms = firms;
	}

	@Transactional(readOnly = true)
	public List<FirmDto> listCurrent() {
		return firms.findCurrentFirms().stream().map(FirmDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<FirmDto> findCurrent(Long firmId) {
		return firms.findCurrentByFirmId(firmId).map(FirmDto::from);
	}

	@Transactional(readOnly = true)
	public List<FirmDto> history(Long firmId) {
		return firms.findByFirmIdentity_FirmIdOrderByFirmHistoryIdAsc(firmId).stream()
				.map(FirmDto::from)
				.toList();
	}
}
