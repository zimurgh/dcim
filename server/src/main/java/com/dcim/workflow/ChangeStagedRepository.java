package com.dcim.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dcim.workflow.assettype.AssetTypeIdentity;

public interface ChangeStagedRepository extends JpaRepository<ChangeStaged, Long> {

	boolean existsByAssetType(AssetTypeIdentity assetType);
}
