package com.dcim.site.cage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dcim.site.datacenter.DataCenterIdentity;
import com.dcim.site.datacenter.DataCenterIdentityRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CageServiceTests {

	@Autowired
	CageService cages;

	@Autowired
	CageIdentityRepository cageIdentities;

	@Autowired
	CageHistoryRepository history;

	@Autowired
	DataCenterIdentityRepository dataCenterIdentities;

	@Test
	void listsAndLoadsCurrentCageUnderDataCenter() {
		DataCenterIdentity dataCenter = dataCenterIdentities.save(new DataCenterIdentity());
		CageIdentity cage = cageIdentities.save(new CageIdentity());

		history.save(new CageHistory(
				cage,
				dataCenter,
				"Cage A",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(cages.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.cageId()).isEqualTo(cage.getCageId());
			assertThat(row.dataCenterId()).isEqualTo(dataCenter.getDataCenterId());
			assertThat(row.cageName()).isEqualTo("Cage A");
		});

		assertThat(cages.listCurrentByDataCenter(dataCenter.getDataCenterId())).hasSize(1);
		assertThat(cages.findCurrent(cage.getCageId())).get()
				.extracting(CageDto::cageName)
				.isEqualTo("Cage A");
		assertThat(cages.history(cage.getCageId())).hasSize(1);
	}
}
