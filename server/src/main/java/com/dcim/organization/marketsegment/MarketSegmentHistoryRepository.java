package com.dcim.organization.marketsegment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MarketSegmentHistoryRepository extends JpaRepository<MarketSegmentHistory, Long> {

	@Query("""
			select m from MarketSegmentHistory m
			where m.validTo is null
			order by m.marketSegmentName
			""")
	List<MarketSegmentHistory> findCurrent();

	@Query("""
			select m from MarketSegmentHistory m
			where m.marketSegmentIdentity.marketSegmentId = :marketSegmentId and m.validTo is null
			""")
	Optional<MarketSegmentHistory> findCurrentByMarketSegmentId(@Param("marketSegmentId") Long marketSegmentId);

	List<MarketSegmentHistory> findByMarketSegmentIdentity_MarketSegmentIdOrderByMarketSegmentHistoryIdAsc(
			Long marketSegmentId);

	@Query("""
			select count(m) > 0 from MarketSegmentHistory m
			where m.validTo is null and m.status = 'Active'
			and lower(m.marketSegmentName) = lower(:name)
			and (:excludeId is null or m.marketSegmentIdentity.marketSegmentId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);

	@Query(value = """
			select distinct x.CROSS_CONNECT_ID
			from T_CROSS_CONNECT_HISTORY x
			where x.VALID_TO is null and x.STATUS = 'Active'
			and x.MARKET_SEGMENT_ID = :marketSegmentId
			""", nativeQuery = true)
	List<Long> findActiveCrossConnectIdsReferencingMarketSegment(@Param("marketSegmentId") Long marketSegmentId);
}
