package com.dcim.asset;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AssetHttp {

	private AssetHttp() {
	}

	public static <T> T requireFound(Optional<T> value, String resource, Object id) {
		return value.orElseThrow(() -> notFound(resource, id));
	}

	public static <T> List<T> requireNonEmpty(List<T> rows, String resource, Object id) {
		if (rows.isEmpty()) {
			throw notFound(resource, id);
		}
		return rows;
	}

	private static ResponseStatusException notFound(String resource, Object id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found: " + id);
	}
}
