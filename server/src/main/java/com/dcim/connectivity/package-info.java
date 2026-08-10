/**
 * Cross-connects/types, charge types, latencies, speeds, market data feeds/types, documents, and cables
 * between firms and ports.
 */
@org.springframework.modulith.ApplicationModule(
		displayName = "Connectivity",
		allowedDependencies = {
				"asset",
				"organization :: firm",
				"organization :: marketsegment",
				"site :: rackdeviceport"
		}
)
package com.dcim.connectivity;
