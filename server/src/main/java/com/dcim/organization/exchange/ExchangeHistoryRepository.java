package com.dcim.organization.exchange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExchangeHistoryRepository extends JpaRepository<ExchangeHistory, Long> {

	@Query("""
			select e from ExchangeHistory e
			where e.validTo is null
			order by e.exchangeName
			""")
	List<ExchangeHistory> findCurrent();

	@Query("""
			select e from ExchangeHistory e
			where e.exchangeIdentity.exchangeId = :exchangeId and e.validTo is null
			""")
	Optional<ExchangeHistory> findCurrentByExchangeId(@Param("exchangeId") Long exchangeId);

	List<ExchangeHistory> findByExchangeIdentity_ExchangeIdOrderByExchangeHistoryIdAsc(Long exchangeId);
}
