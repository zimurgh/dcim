package com.dcim.connectivity.speed;

import jakarta.persistence.*;

@Entity
@Table(name = "T_SPEED_IDENTITY")
public class SpeedIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SPEED_ID", nullable = false)
	private Long speedId;

	public SpeedIdentity() {
	}

	public Long getSpeedId() {
		return speedId;
	}
}
