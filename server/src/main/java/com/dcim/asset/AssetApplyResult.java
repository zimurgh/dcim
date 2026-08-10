package com.dcim.asset;

import java.util.List;

public record AssetApplyResult(Long assetIdentityId, List<AssetHistoryLink> links) {
}
