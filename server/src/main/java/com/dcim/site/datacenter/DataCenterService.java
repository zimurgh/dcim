package com.dcim.site.datacenter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataCenterService {

	private final DataCenterHistoryRepository dataCenters;

	DataCenterService(DataCenterHistoryRepository dataCenters) {
		this.dataCenters = dataCenters;
	}

	@Transactional(readOnly = true)
	public List<DataCenterDto> listCurrent() {
		return dataCenters.findCurrentDataCenters().stream().map(DataCenterDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<DataCenterDto> findCurrent(Long dataCenterId) {
		return dataCenters.findCurrentByDataCenterId(dataCenterId).map(DataCenterDto::from);
	}

	@Transactional(readOnly = true)
	public List<DataCenterDto> history(Long dataCenterId) {
		return dataCenters.findByDataCenterIdentity_DataCenterIdOrderByDataCenterHistoryIdAsc(dataCenterId).stream()
				.map(DataCenterDto::from)
				.toList();
	}
}
