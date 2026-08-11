package com.dcim.connectivity.crossconnect;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrossConnectService {

	private final CrossConnectHistoryRepository crossConnects;
	private final CrossConnectViewRepository crossConnectViews;

	CrossConnectService(
			CrossConnectHistoryRepository crossConnects,
			CrossConnectViewRepository crossConnectViews) {
		this.crossConnects = crossConnects;
		this.crossConnectViews = crossConnectViews;
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> listCurrent() {
		return crossConnectViews.findCurrent().stream().map(CrossConnectDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<CrossConnectDto> findCurrent(Long crossConnectId) {
		return crossConnectViews.findCurrentByCrossConnectId(crossConnectId).map(CrossConnectDto::from);
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> history(Long crossConnectId) {
		return crossConnects.findByCrossConnectIdentity_CrossConnectIdOrderByCrossConnectHistoryIdAsc(crossConnectId)
				.stream()
				.map(CrossConnectDto::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> listCurrentByLatencyId(Long latencyId) {
		return crossConnectViews.findCurrentByLatencyId(latencyId).stream().map(CrossConnectDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> listCurrentBySpeedId(Long speedId) {
		return crossConnectViews.findCurrentBySpeedId(speedId).stream().map(CrossConnectDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<CrossConnectDto> listCurrentByCrossConnectTypeId(Long crossConnectTypeId) {
		return crossConnectViews.findCurrentByCrossConnectTypeId(crossConnectTypeId).stream()
				.map(CrossConnectDto::from)
				.toList();
	}
}
