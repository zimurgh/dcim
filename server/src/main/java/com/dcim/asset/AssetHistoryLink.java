package com.dcim.asset;

public record AssetHistoryLink(Long historyId, String role) {

	public static final String ROLE_CREATED = "CREATED";
	public static final String ROLE_CLOSED_PRIOR = "CLOSED_PRIOR";
}
