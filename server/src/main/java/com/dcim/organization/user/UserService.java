package com.dcim.organization.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserHistoryRepository users;

	UserService(UserHistoryRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<UserDto> listCurrent() {
		return users.findCurrentUsers().stream().map(UserDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<UserDto> findCurrent(Long userId) {
		return users.findCurrentByUserId(userId).map(UserDto::from);
	}

	@Transactional(readOnly = true)
	public List<UserDto> history(Long userId) {
		return users.findByUserIdentity_UserIdOrderByUserHistoryIdAsc(userId).stream()
				.map(UserDto::from)
				.toList();
	}
}
