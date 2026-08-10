package com.dcim.connectivity.cable;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CableService {

	private final CableHistoryRepository cables;

	CableService(CableHistoryRepository cables) {
		this.cables = cables;
	}

	@Transactional(readOnly = true)
	public List<CableDto> listCurrent() {
		return cables.findCurrent().stream().map(CableDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<CableDto> listCurrentByCrossConnect(Long crossConnectId) {
		return cables.findCurrentByCrossConnectId(crossConnectId).stream().map(CableDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<CableDto> findCurrent(Long cableId) {
		return cables.findCurrentByCableId(cableId).map(CableDto::from);
	}

	@Transactional(readOnly = true)
	public List<CableDto> history(Long cableId) {
		return cables.findByCableIdentity_CableIdOrderByCableHistoryIdAsc(cableId).stream()
				.map(CableDto::from)
				.toList();
	}
}
