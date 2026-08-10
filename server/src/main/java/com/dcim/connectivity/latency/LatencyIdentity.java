package com.dcim.connectivity.latency;

import jakarta.persistence.*;

@Entity
@Table(name = "T_LATENCY_IDENTITY")
public class LatencyIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "LATENCY_ID", nullable = false)
	private Long latencyId;

	public LatencyIdentity() {
	}

	public Long getLatencyId() {
		return latencyId;
	}
}
