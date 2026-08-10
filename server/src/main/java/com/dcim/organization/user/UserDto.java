package com.dcim.organization.user;

import java.time.Instant;
import java.time.LocalDate;

public record UserDto(
		Long userId,
		Long userHistoryId,
		String userName,
		boolean isInitiator,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static UserDto from(UserHistory history) {
		return new UserDto(
				history.getUserId(),
				history.getUserHistoryId(),
				history.getUserName(),
				history.isInitiator(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
