package com.dcim.workflow.assettype;

import java.util.List;
import java.util.Optional;

import com.dcim.workflow.WorkflowException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetTypeService {

	private final AssetTypeHistoryRepository types;

	AssetTypeService(AssetTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<AssetTypeDto> listCurrent() {
		return types.findCurrent().stream().map(AssetTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<AssetTypeDto> findCurrent(Long assetTypeId) {
		return types.findCurrentByAssetTypeId(assetTypeId).map(AssetTypeDto::from);
	}

	@Transactional(readOnly = true)
	public Optional<AssetTypeDto> findCurrentByCode(String code) {
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		return types.findCurrentByCode(code).map(AssetTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<AssetTypeDto> history(Long assetTypeId) {
		return types.findByAssetTypeIdentity_AssetTypeIdOrderByAssetTypeHistoryIdAsc(assetTypeId).stream()
				.map(AssetTypeDto::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public int applyRank(String assetTypeCode) {
		return types.findCurrentByCode(assetTypeCode)
				.map(AssetTypeHistory::getApplyRank)
				.orElseGet(() -> com.dcim.workflow.AssetApplyOrder.rank(assetTypeCode));
	}

	@Transactional(readOnly = true)
	public int applyRank(AssetTypeIdentity identity) {
		return types.findCurrentByAssetTypeId(identity.getAssetTypeId())
				.map(AssetTypeHistory::getApplyRank)
				.orElseThrow(() -> new WorkflowException("Unknown asset type id: " + identity.getAssetTypeId()));
	}

	@Transactional(readOnly = true)
	public String requireCode(AssetTypeIdentity identity) {
		return types.findCurrentByAssetTypeId(identity.getAssetTypeId())
				.map(AssetTypeHistory::getAssetTypeCode)
				.orElseThrow(() -> new WorkflowException("Unknown asset type id: " + identity.getAssetTypeId()));
	}

	@Transactional(readOnly = true)
	public AssetTypeIdentity requireIdentity(String assetTypeCode) {
		if (assetTypeCode == null || assetTypeCode.isBlank()) {
			throw new WorkflowException("Staging requires assetType");
		}
		return types.findCurrentByCode(assetTypeCode)
				.map(AssetTypeHistory::getAssetTypeIdentity)
				.orElseThrow(() -> new WorkflowException("Unknown asset type: " + assetTypeCode));
	}

	@Transactional(readOnly = true)
	public AssetTypeHistory requireCurrent(Long assetTypeId) {
		return types.findCurrentByAssetTypeId(assetTypeId)
				.orElseThrow(() -> new WorkflowException("Unknown asset type id: " + assetTypeId));
	}
}
