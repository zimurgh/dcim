package com.dcim.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeCommittedRepository extends JpaRepository<ChangeCommitted, Long> {
}
