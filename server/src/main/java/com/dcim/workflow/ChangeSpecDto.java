package com.dcim.workflow;

import java.time.Instant;
import java.util.List;

public record ChangeSpecDto(
		Long changeSpecId,
		Long ownerFirmId,
		String name,
		ChangeSpecStatus status,
		Instant createdAt,
		String createdBy,
		List<Long> changeIds,
		List<ChrecDto> chrecs) {

	public record ChrecDto(Long chrecId, String jiraKey, String title, String url) {
		static ChrecDto from(Chrec chrec) {
			return new ChrecDto(chrec.getChrecId(), chrec.getJiraKey(), chrec.getTitle(), chrec.getUrl());
		}
	}
}
