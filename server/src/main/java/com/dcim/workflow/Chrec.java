package com.dcim.workflow;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHREC")
public class Chrec {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CHREC_ID", nullable = false)
	private Long chrecId;

	@Column(name = "JIRA_KEY", nullable = false, length = 50, unique = true)
	private String jiraKey;

	@Column(name = "TITLE", length = 200)
	private String title;

	@Column(name = "URL", length = 500)
	private String url;

	protected Chrec() {
	}

	public Chrec(String jiraKey, String title, String url) {
		this.jiraKey = jiraKey;
		this.title = title;
		this.url = url;
	}

	public Long getChrecId() {
		return chrecId;
	}

	public String getJiraKey() {
		return jiraKey;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
}
