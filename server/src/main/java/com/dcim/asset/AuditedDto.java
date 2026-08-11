package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface AuditedDto {

	AuditSlice audit();

	@JsonIgnore
	default LocalDate validFrom() {
		return audit().validFrom();
	}

	@JsonIgnore
	default LocalDate validTo() {
		return audit().validTo();
	}

	@JsonIgnore
	default Instant appliedAt() {
		return audit().appliedAt();
	}

	@JsonIgnore
	default Long appliedBy() {
		return audit().appliedBy();
	}

	@JsonIgnore
	default String action() {
		return audit().action();
	}

	@JsonIgnore
	default String status() {
		return audit().status();
	}
}
