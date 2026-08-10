package com.dcim.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeSpecChrecRepository extends JpaRepository<ChangeSpecChrec, ChangeSpecChrec.Pk> {

	List<ChangeSpecChrec> findByChangeSpec_ChangeSpecId(Long changeSpecId);

	long countByChangeSpec_ChangeSpecId(Long changeSpecId);

	void deleteByChangeSpec_ChangeSpecIdAndChrec_ChrecId(Long changeSpecId, Long chrecId);
}
