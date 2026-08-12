package com.dcim.workflow.assettype;

import jakarta.persistence.*;

@Entity
@Table(name = "T_ASSET_TYPE_IDENTITY")
public class AssetTypeIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ASSET_TYPE_ID", nullable = false)
	private Long assetTypeId;

	public AssetTypeIdentity() {
	}

	public Long getAssetTypeId() {
		return assetTypeId;
	}
}
