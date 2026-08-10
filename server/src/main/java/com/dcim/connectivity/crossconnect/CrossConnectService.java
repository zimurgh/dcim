package com.dcim.connectivity.crossconnect;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrossConnectService {

	private final CrossConnectHistoryRepository crossConnects;

	CrossConnectService(CrossConnectHistoryRepository crossConnects) {
		this.crossConnects = crossConnects;
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> listCurrent() {
		return crossConnects.findCurrent().stream().map(CrossConnectDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<CrossConnectDto> findCurrent(Long crossConnectId) {
		return crossConnects.findCurrentByCrossConnectId(crossConnectId).map(CrossConnectDto::from);
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> history(Long crossConnectId) {
		return crossConnects.findByCrossConnectIdentity_CrossConnectIdOrderByCrossConnectHistoryIdAsc(crossConnectId)
				.stream()
				.map(CrossConnectDto::from)
				.toList();
	}
}
