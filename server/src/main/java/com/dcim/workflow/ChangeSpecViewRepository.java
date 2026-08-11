package com.dcim.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ChangeSpecViewRepository extends JpaRepository<ChangeSpecView, Long> {

	@Query("""
			select s from ChangeSpecView s
			where s.ownerFirmId = :ownerFirmId
			order by s.changeSpecId
			""")
	List<ChangeSpecView> findByOwnerFirmIdOrderByChangeSpecIdAsc(@Param("ownerFirmId") Long ownerFirmId);

	@Query("""
			select s from ChangeSpecView s
			order by s.changeSpecId
			""")
	List<ChangeSpecView> findAllOrderByChangeSpecIdAsc();
}
