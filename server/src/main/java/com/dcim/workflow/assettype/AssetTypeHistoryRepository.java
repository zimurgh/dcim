package com.dcim.workflow.assettype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssetTypeHistoryRepository extends JpaRepository<AssetTypeHistory, Long> {

	@Query("""
			select t from AssetTypeHistory t
			where t.validTo is null
			order by t.applyRank, t.assetTypeName
			""")
	List<AssetTypeHistory> findCurrent();

	@Query("""
			select t from AssetTypeHistory t
			where t.assetTypeIdentity.assetTypeId = :assetTypeId and t.validTo is null
			""")
	Optional<AssetTypeHistory> findCurrentByAssetTypeId(@Param("assetTypeId") Long assetTypeId);

	@Query("""
			select t from AssetTypeHistory t
			where t.assetTypeCode = :code and t.validTo is null
			""")
	Optional<AssetTypeHistory> findCurrentByCode(@Param("code") String code);

	List<AssetTypeHistory> findByAssetTypeIdentity_AssetTypeIdOrderByAssetTypeHistoryIdAsc(Long assetTypeId);
}
