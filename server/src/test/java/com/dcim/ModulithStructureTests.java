package com.dcim;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithStructureTests {

	@Test
	void verifiesModularStructure() {
		ApplicationModules modules = ApplicationModules.of(ServerApplication.class).verify();
		new Documenter(modules).writeDocumentation();
	}
}
