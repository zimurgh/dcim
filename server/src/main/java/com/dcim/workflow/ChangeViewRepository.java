package com.dcim.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ChangeViewRepository extends JpaRepository<ChangeView, Long> {

	@Query("""
			select c from ChangeView c
			order by c.eventAt desc, c.changeId desc
			""")
	List<ChangeView> findAllLatestFirst();

	@Query("""
			select c from ChangeView c
			where c.changeSpecId = :changeSpecId
			order by c.eventAt desc, c.changeId desc
			""")
	List<ChangeView> findByChangeSpecIdLatestFirst(@Param("changeSpecId") Long changeSpecId);
}
