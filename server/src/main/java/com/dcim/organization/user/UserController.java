package com.dcim.organization.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final UserService users;

	UserController(UserService users) {
		this.users = users;
	}

	@GetMapping
	List<UserDto> list() {
		return users.listCurrent();
	}

	@GetMapping("/{userId}")
	UserDto get(@PathVariable Long userId) {
		return users.findCurrent(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
	}

	@GetMapping("/{userId}/history")
	List<UserDto> history(@PathVariable Long userId) {
		List<UserDto> rows = users.history(userId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
		}
		return rows;
	}
}
