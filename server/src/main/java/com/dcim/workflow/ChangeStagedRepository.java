package com.dcim.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeStagedRepository extends JpaRepository<ChangeStaged, Long> {
}
