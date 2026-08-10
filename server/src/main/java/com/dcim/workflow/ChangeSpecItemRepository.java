package com.dcim.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeSpecItemRepository extends JpaRepository<ChangeSpecItem, ChangeSpecItem.Pk> {

	List<ChangeSpecItem> findByChangeSpec_ChangeSpecId(Long changeSpecId);

	List<ChangeSpecItem> findByChangeIdentity_ChangeId(Long changeId);

	boolean existsByChangeIdentity_ChangeId(Long changeId);

	void deleteByChangeSpec_ChangeSpecIdAndChangeIdentity_ChangeId(Long changeSpecId, Long changeId);
}
