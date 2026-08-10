package com.dcim.organization.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(users.findCurrent(userId), "User", userId);
	}

	@GetMapping("/{userId}/history")
	List<UserDto> history(@PathVariable Long userId) {
		return AssetHttp.requireNonEmpty(users.history(userId), "User", userId);
	}
}
