/**
 * Change and Change Spec workflow — promotion, specs, CHRECs, apply.
 * Changes, Change Specs, CHRECs, promotion, validation orchestration, and apply.
 * Asset-type validate/apply lives in domain modules via AssetChangeValidator / AssetChangeApplier.
 */
@org.springframework.modulith.ApplicationModule(
		displayName = "Workflow",
		allowedDependencies = { "asset", "organization :: firm" }
)
package com.dcim.workflow;
