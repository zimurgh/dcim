package com.dcim.organization.user;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record UserDto(
		Long userId,
		Long userHistoryId,
		String userName,
		boolean isInitiator,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static UserDto from(UserHistory history) {
		return new UserDto(
				history.getUserId(),
				history.getUserHistoryId(),
				history.getUserName(),
				history.isInitiator(),
				AuditSlice.from(history));
	}
}
