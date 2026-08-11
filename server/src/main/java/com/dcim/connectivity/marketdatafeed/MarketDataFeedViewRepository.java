package com.dcim.connectivity.marketdatafeed;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MarketDataFeedViewRepository extends JpaRepository<MarketDataFeedView, Long> {

	@Query("""
			select f from MarketDataFeedView f
			where f.validTo is null
			order by f.marketDataFeedName
			""")
	List<MarketDataFeedView> findCurrent();

	@Query("""
			select f from MarketDataFeedView f
			where f.marketDataFeedIdentity.marketDataFeedId = :marketDataFeedId and f.validTo is null
			""")
	Optional<MarketDataFeedView> findCurrentByMarketDataFeedId(@Param("marketDataFeedId") Long marketDataFeedId);

	@Query("""
			select f from MarketDataFeedView f
			where f.crossConnectIdentity.crossConnectId = :crossConnectId and f.validTo is null
			order by f.marketDataFeedName
			""")
	List<MarketDataFeedView> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	@Query("""
			select f from MarketDataFeedView f
			where f.marketDataFeedTypeId = :marketDataFeedTypeId and f.validTo is null
			""")
	List<MarketDataFeedView> findCurrentByMarketDataFeedTypeId(
			@Param("marketDataFeedTypeId") Long marketDataFeedTypeId);

	List<MarketDataFeedView> findByMarketDataFeedIdentity_MarketDataFeedIdOrderByMarketDataFeedHistoryIdAsc(
			Long marketDataFeedId);
}
