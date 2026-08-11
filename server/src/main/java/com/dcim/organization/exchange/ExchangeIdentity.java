package com.dcim.organization.exchange;

import jakarta.persistence.*;

@Entity
@Table(name = "T_EXCHANGE_IDENTITY")
public class ExchangeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EXCHANGE_ID", nullable = false)
	private Long exchangeId;

	public ExchangeIdentity() {
	}

	public Long getExchangeId() {
		return exchangeId;
	}
}
