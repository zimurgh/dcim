package com.dcim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies Liquibase demo-seed.sql against an isolated H2 database (MariaDB mode).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.liquibase.contexts=demo",
		"spring.datasource.url=jdbc:h2:mem:dcim-demo;MODE=MariaDB;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
})
@Transactional
class DemoSeedTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void demoSeedLoadsCoreInventoryAndWorkflowRows() {
		assertThat(jdbc.queryForObject("select count(*) from T_FIRM_IDENTITY", Integer.class)).isEqualTo(4);
		assertThat(jdbc.queryForObject("select count(*) from T_DATA_CENTER_IDENTITY", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from T_CROSS_CONNECT_IDENTITY", Integer.class)).isEqualTo(3);
		assertThat(jdbc.queryForObject("select count(*) from T_CHANGE_SPEC", Integer.class)).isEqualTo(3);
		assertThat(jdbc.queryForObject(
				"select count(*) from V_CHANGE_SPEC where STATUS = 'PENDING_BILLING'",
				Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from V_CHANGE", Integer.class)).isEqualTo(9);
		assertThat(jdbc.queryForObject(
				"select OWNER_FIRM_NAME from V_CHANGE_SPEC where CHANGE_SPEC_ID = 101",
				String.class)).isEqualTo("Acme Trading");
	}
}
