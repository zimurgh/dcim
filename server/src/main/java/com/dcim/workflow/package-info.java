/**
 * Change and Change Spec workflow — promotion, specs, CHRECs, apply.
 * Asset-type apply lives in domain modules (organization, site, connectivity) via AssetChangeApplier.
 */
@org.springframework.modulith.ApplicationModule(
		displayName = "Workflow",
		allowedDependencies = { "asset", "organization :: firm" }
)
package com.dcim.workflow;
