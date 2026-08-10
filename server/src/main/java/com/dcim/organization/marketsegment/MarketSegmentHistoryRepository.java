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
}
