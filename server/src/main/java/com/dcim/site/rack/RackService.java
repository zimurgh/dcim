package com.dcim.site.rack;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RackService {

	private final RackHistoryRepository racks;

	RackService(RackHistoryRepository racks) {
		this.racks = racks;
	}

	@Transactional(readOnly = true)
	public List<RackDto> listCurrent() {
		return racks.findCurrentRacks().stream().map(RackDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<RackDto> listCurrentByCage(Long cageId) {
		return racks.findCurrentByCageId(cageId).stream().map(RackDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<RackDto> findCurrent(Long rackId) {
		return racks.findCurrentByRackId(rackId).map(RackDto::from);
	}

	@Transactional(readOnly = true)
	public List<RackDto> history(Long rackId) {
		return racks.findByRackIdentity_RackIdOrderByRackHistoryIdAsc(rackId).stream()
				.map(RackDto::from)
				.toList();
	}
}
