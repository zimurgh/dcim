package com.dcim.connectivity.crossconnecttype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrossConnectTypeService {

	private final CrossConnectTypeHistoryRepository types;

	CrossConnectTypeService(CrossConnectTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<CrossConnectTypeDto> listCurrent() {
		return types.findCurrent().stream().map(CrossConnectTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<CrossConnectTypeDto> findCurrent(Long crossConnectTypeId) {
		return types.findCurrentByCrossConnectTypeId(crossConnectTypeId).map(CrossConnectTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<CrossConnectTypeDto> history(Long crossConnectTypeId) {
		return types
				.findByCrossConnectTypeIdentity_CrossConnectTypeIdOrderByCrossConnectTypeHistoryIdAsc(
						crossConnectTypeId)
				.stream()
				.map(CrossConnectTypeDto::from)
				.toList();
	}
}
