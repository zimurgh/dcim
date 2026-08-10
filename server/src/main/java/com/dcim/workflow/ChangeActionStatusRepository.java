package com.dcim.workflow;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeActionStatusRepository extends JpaRepository<ChangeActionStatus, ChangeActionStatus.Pk> {

	Optional<ChangeActionStatus> findByActionAndStage(ChangeAction action, ChangeStage stage);
}
