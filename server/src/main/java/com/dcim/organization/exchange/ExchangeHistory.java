package com.dcim.organization.exchange;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.asset.AuditHistory;

import jakarta.persistence.*;

@Entity
@Table(name = "T_EXCHANGE_HISTORY")
public class ExchangeHistory extends AuditHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EXCHANGE_HISTORY_ID", nullable = false)
	private Long exchangeHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "EXCHANGE_ID", nullable = false)
	private ExchangeIdentity exchangeIdentity;

	@Column(name = "EXCHANGE_NAME", nullable = false, length = 100)
	private String exchangeName;

	@Column(name = "EXCHANGE_CODE", nullable = false, length = 50)
	private String exchangeCode;

	@Column(name = "EXCHANGE_ABBREVIATION", nullable = false, length = 50)
	private String exchangeAbbreviation;

	@Enumerated(EnumType.STRING)
	@Column(name = "EXCHANGE_TYPE", nullable = false, length = 50)
	private ExchangeType exchangeType;

	protected ExchangeHistory() {
	}

	public ExchangeHistory(
			ExchangeIdentity exchangeIdentity,
			String exchangeName,
			String exchangeCode,
			String exchangeAbbreviation,
			ExchangeType exchangeType,
			LocalDate validFrom,
			LocalDate validTo,
			Instant appliedAt,
			Long appliedBy,
			String action,
			String status) {
		super(validFrom, validTo, appliedAt, appliedBy, action, status);
		this.exchangeIdentity = exchangeIdentity;
		this.exchangeName = exchangeName;
		this.exchangeCode = exchangeCode;
		this.exchangeAbbreviation = exchangeAbbreviation;
		this.exchangeType = exchangeType;
	}

	public Long getExchangeHistoryId() {
		return exchangeHistoryId;
	}

	public ExchangeIdentity getExchangeIdentity() {
		return exchangeIdentity;
	}

	public Long getExchangeId() {
		return exchangeIdentity.getExchangeId();
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public String getExchangeCode() {
		return exchangeCode;
	}

	public String getExchangeAbbreviation() {
		return exchangeAbbreviation;
	}

	public ExchangeType getExchangeType() {
		return exchangeType;
	}
}
