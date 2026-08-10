package com.dcim.asset;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonPayloads {

	private final ObjectMapper json;

	JsonPayloads(ObjectMapper json) {
		this.json = json;
	}

	public JsonNode read(String payloadJson) {
		try {
			return json.readTree(payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
		}
		catch (RuntimeException ex) {
			throw new AssetApplyException("Invalid payload JSON", ex);
		}
	}

	public static String requiredText(JsonNode body, String field) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull() || node.asString().isBlank()) {
			throw new AssetApplyException("Payload missing " + field);
		}
		return node.asString();
	}

	public static String textOrNull(JsonNode body, String field) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		String value = node.asString();
		return value.isBlank() ? null : value;
	}

	public static Long requiredLong(JsonNode body, String field) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull() || !node.canConvertToLong()) {
			throw new AssetApplyException("Payload missing " + field);
		}
		return node.asLong();
	}

	public static Long longOrNull(JsonNode body, String field) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.canConvertToLong()) {
			throw new AssetApplyException("Payload field " + field + " must be a number");
		}
		return node.asLong();
	}

	public static boolean booleanOrDefault(JsonNode body, String field, boolean defaultValue) {
		JsonNode node = body.get(field);
		if (node == null || node.isNull()) {
			return defaultValue;
		}
		return node.asBoolean();
	}
}
