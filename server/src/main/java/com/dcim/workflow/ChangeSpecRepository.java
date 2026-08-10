package com.dcim.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeSpecRepository extends JpaRepository<ChangeSpec, Long> {

	List<ChangeSpec> findByOwnerFirm_FirmIdOrderByChangeSpecIdAsc(Long firmId);
}
