package com.dcim.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dcim.workflow.assettype.AssetTypeIdentity;

public interface ChangeCommittedRepository extends JpaRepository<ChangeCommitted, Long> {

	boolean existsByAssetType(AssetTypeIdentity assetType);
}
