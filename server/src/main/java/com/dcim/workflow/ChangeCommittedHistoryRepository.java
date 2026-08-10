package com.dcim.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeCommittedHistoryRepository extends JpaRepository<ChangeCommittedHistory, Long> {

	List<ChangeCommittedHistory> findByCommitted_ChangeId(Long changeId);
}
