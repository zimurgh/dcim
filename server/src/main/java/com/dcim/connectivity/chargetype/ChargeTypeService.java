package com.dcim.connectivity.chargetype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeTypeService {

	private final ChargeTypeHistoryRepository types;

	ChargeTypeService(ChargeTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<ChargeTypeDto> listCurrent() {
		return types.findCurrent().stream().map(ChargeTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<ChargeTypeDto> findCurrent(Long chargeTypeId) {
		return types.findCurrentByChargeTypeId(chargeTypeId).map(ChargeTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<ChargeTypeDto> history(Long chargeTypeId) {
		return types.findByChargeTypeIdentity_ChargeTypeIdOrderByChargeTypeHistoryIdAsc(chargeTypeId).stream()
				.map(ChargeTypeDto::from)
				.toList();
	}
}
