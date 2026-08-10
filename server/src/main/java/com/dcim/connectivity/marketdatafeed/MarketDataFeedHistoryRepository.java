package com.dcim.connectivity.marketdatafeed;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MarketDataFeedHistoryRepository extends JpaRepository<MarketDataFeedHistory, Long> {

	@Query("""
			select m from MarketDataFeedHistory m
			where m.validTo is null
			order by m.marketDataFeedName
			""")
	List<MarketDataFeedHistory> findCurrent();

	@Query("""
			select m from MarketDataFeedHistory m
			where m.marketDataFeedIdentity.marketDataFeedId = :marketDataFeedId and m.validTo is null
			""")
	Optional<MarketDataFeedHistory> findCurrentByMarketDataFeedId(
			@Param("marketDataFeedId") Long marketDataFeedId);

	@Query("""
			select m from MarketDataFeedHistory m
			where m.crossConnectIdentity.crossConnectId = :crossConnectId and m.validTo is null
			order by m.marketDataFeedName
			""")
	List<MarketDataFeedHistory> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	List<MarketDataFeedHistory> findByMarketDataFeedIdentity_MarketDataFeedIdOrderByMarketDataFeedHistoryIdAsc(
			Long marketDataFeedId);
}
