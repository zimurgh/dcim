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
}
