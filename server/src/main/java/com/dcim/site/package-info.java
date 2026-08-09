/**
 * Site inventory — spatial hierarchy (data centers, cages, racks, devices, ports).
 */
@org.springframework.modulith.ApplicationModule(
		displayName = "Site",
		allowedDependencies = "asset"
)
package com.dcim.site;
