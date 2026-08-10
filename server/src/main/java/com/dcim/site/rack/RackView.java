package com.dcim.site.rack;

import com.dcim.asset.AuditHistory;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

/**
 * Read model for rack history with cage and data center names flattened
 * instead of a {@link com.dcim.site.cage.CageIdentity} association.
 */
@Entity
@Immutable
@Table(name = "V_RACK")
public class RackView extends AuditHistory {

	@Id
	@Column(name = "RACK_HISTORY_ID", nullable = false)
	private Long rackHistoryId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "RACK_ID", nullable = false)
	private RackIdentity rackIdentity;

	@Column(name = "CAGE_ID", nullable = false)
	private Long cageId;

	@Column(name = "CAGE_NAME", nullable = false, length = 50)
	private String cageName;

	@Column(name = "DATA_CENTER_ID", nullable = false)
	private Long dataCenterId;

	@Column(name = "DATA_CENTER_NAME", nullable = false, length = 50)
	private String dataCenterName;

	@Column(name = "RACK_NAME", nullable = false, length = 50)
	private String rackName;

	protected RackView() {
	}

	public Long getRackHistoryId() {
		return rackHistoryId;
	}

	public RackIdentity getRackIdentity() {
		return rackIdentity;
	}

	public Long getRackId() {
		return rackIdentity.getRackId();
	}

	public Long getCageId() {
		return cageId;
	}

	public String getCageName() {
		return cageName;
	}

	public Long getDataCenterId() {
		return dataCenterId;
	}

	public String getDataCenterName() {
		return dataCenterName;
	}

	public String getRackName() {
		return rackName;
	}
}
