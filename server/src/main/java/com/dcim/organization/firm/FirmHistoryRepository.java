package com.dcim.organization.firm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FirmHistoryRepository extends JpaRepository<FirmHistory, Long> {

	@Query("""
			select f from FirmHistory f
			where f.validTo is null
			order by f.firmName
			""")
	List<FirmHistory> findCurrentFirms();

	@Query("""
			select f from FirmHistory f
			where f.firmIdentity.firmId = :firmId and f.validTo is null
			""")
	Optional<FirmHistory> findCurrentByFirmId(@Param("firmId") Long firmId);

	List<FirmHistory> findByFirmIdentity_FirmIdOrderByFirmHistoryIdAsc(Long firmId);

	@Query("""
			select count(f) > 0 from FirmHistory f
			where f.validTo is null and f.status = 'Active'
			and lower(f.firmName) = lower(:name)
			and (:excludeId is null or f.firmIdentity.firmId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);

	@Query(value = """
			select distinct x.CROSS_CONNECT_ID
			from T_CROSS_CONNECT_HISTORY x
			where x.VALID_TO is null and x.STATUS = 'Active'
			and (x.OWNER_FIRM_ID = :firmId or x.BILLING_FIRM_ID = :firmId or x.PROVIDER_FIRM_ID = :firmId)
			""", nativeQuery = true)
	List<Long> findActiveCrossConnectIdsReferencingFirm(@Param("firmId") Long firmId);

	@Query(value = """
			select distinct m.MARKET_DATA_FEED_ID
			from T_MARKET_DATA_FEED_HISTORY m
			where m.VALID_TO is null and m.STATUS = 'Active'
			and (m.OWNER_FIRM_ID = :firmId or m.BILLING_FIRM_ID = :firmId or m.PROVIDER_FIRM_ID = :firmId)
			""", nativeQuery = true)
	List<Long> findActiveMarketDataFeedIdsReferencingFirm(@Param("firmId") Long firmId);
}
