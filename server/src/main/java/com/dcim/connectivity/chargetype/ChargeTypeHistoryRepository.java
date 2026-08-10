package com.dcim.connectivity.chargetype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ChargeTypeHistoryRepository extends JpaRepository<ChargeTypeHistory, Long> {

	@Query("""
			select t from ChargeTypeHistory t
			where t.validTo is null
			order by t.chargeTypeName
			""")
	List<ChargeTypeHistory> findCurrent();

	@Query("""
			select t from ChargeTypeHistory t
			where t.chargeTypeIdentity.chargeTypeId = :chargeTypeId and t.validTo is null
			""")
	Optional<ChargeTypeHistory> findCurrentByChargeTypeId(@Param("chargeTypeId") Long chargeTypeId);

	List<ChargeTypeHistory> findByChargeTypeIdentity_ChargeTypeIdOrderByChargeTypeHistoryIdAsc(Long chargeTypeId);
}
