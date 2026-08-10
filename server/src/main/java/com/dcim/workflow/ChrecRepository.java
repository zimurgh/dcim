package com.dcim.workflow;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChrecRepository extends JpaRepository<Chrec, Long> {

	Optional<Chrec> findByJiraKey(String jiraKey);
}
