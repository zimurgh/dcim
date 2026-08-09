package com.dcim.site.cage;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CageService {

	private final CageHistoryRepository cages;

	CageService(CageHistoryRepository cages) {
		this.cages = cages;
	}

	@Transactional(readOnly = true)
	public List<CageDto> listCurrent() {
		return cages.findCurrentCages().stream().map(CageDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<CageDto> listCurrentByDataCenter(Long dataCenterId) {
		return cages.findCurrentByDataCenterId(dataCenterId).stream().map(CageDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<CageDto> findCurrent(Long cageId) {
		return cages.findCurrentByCageId(cageId).map(CageDto::from);
	}

	@Transactional(readOnly = true)
	public List<CageDto> history(Long cageId) {
		return cages.findByCageIdentity_CageIdOrderByCageHistoryIdAsc(cageId).stream()
				.map(CageDto::from)
				.toList();
	}
}
