package com.dcim.connectivity.marketdatafeedtype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MarketDataFeedTypeHistoryRepository extends JpaRepository<MarketDataFeedTypeHistory, Long> {

	@Query("""
			select t from MarketDataFeedTypeHistory t
			where t.validTo is null
			order by t.marketDataFeedTypeName
			""")
	List<MarketDataFeedTypeHistory> findCurrent();

	@Query("""
			select t from MarketDataFeedTypeHistory t
			where t.marketDataFeedTypeIdentity.marketDataFeedTypeId = :marketDataFeedTypeId and t.validTo is null
			""")
	Optional<MarketDataFeedTypeHistory> findCurrentByMarketDataFeedTypeId(
			@Param("marketDataFeedTypeId") Long marketDataFeedTypeId);

	@Query("""
			select t from MarketDataFeedTypeHistory t
			where t.chargeTypeIdentity.chargeTypeId = :chargeTypeId and t.validTo is null
			""")
	List<MarketDataFeedTypeHistory> findCurrentByChargeTypeId(@Param("chargeTypeId") Long chargeTypeId);

	List<MarketDataFeedTypeHistory>
			findByMarketDataFeedTypeIdentity_MarketDataFeedTypeIdOrderByMarketDataFeedTypeHistoryIdAsc(
					Long marketDataFeedTypeId);
}
