package com.dcim.connectivity.crossconnecttype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CrossConnectTypeHistoryRepository extends JpaRepository<CrossConnectTypeHistory, Long> {

	@Query("""
			select t from CrossConnectTypeHistory t
			where t.validTo is null
			order by t.crossConnectTypeName
			""")
	List<CrossConnectTypeHistory> findCurrent();

	@Query("""
			select t from CrossConnectTypeHistory t
			where t.crossConnectTypeIdentity.crossConnectTypeId = :crossConnectTypeId and t.validTo is null
			""")
	Optional<CrossConnectTypeHistory> findCurrentByCrossConnectTypeId(
			@Param("crossConnectTypeId") Long crossConnectTypeId);

	@Query("""
			select t from CrossConnectTypeHistory t
			where t.chargeTypeIdentity.chargeTypeId = :chargeTypeId and t.validTo is null
			""")
	List<CrossConnectTypeHistory> findCurrentByChargeTypeId(@Param("chargeTypeId") Long chargeTypeId);

	List<CrossConnectTypeHistory>
			findByCrossConnectTypeIdentity_CrossConnectTypeIdOrderByCrossConnectTypeHistoryIdAsc(
					Long crossConnectTypeId);
}
