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
