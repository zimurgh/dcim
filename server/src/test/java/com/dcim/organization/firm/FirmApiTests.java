package com.dcim.organization.firm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FirmApiTests extends ChangeTestSupport {

	@Autowired
	MockMvc mvc;

	@Test
	void listGetAndHistoryReturnCurrentRows() throws Exception {
		Long firmId = seedFirm(unique("Acme"));

		mvc.perform(get("/api/firms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.firmId == %d)].firmName", firmId).exists());

		mvc.perform(get("/api/firms/{id}", firmId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firmId").value(firmId))
				.andExpect(jsonPath("$.validFrom").exists())
				.andExpect(jsonPath("$.status").value("Active"));

		mvc.perform(get("/api/firms/{id}/history", firmId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].firmId").value(firmId));
	}

	@Test
	void missingFirmReturns404() throws Exception {
		mvc.perform(get("/api/firms/{id}", 999_999_999L))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/firms/{id}/history", 999_999_999L))
				.andExpect(status().isNotFound());
	}

	@Test
	void historyIncludesPriorClosedRows() throws Exception {
		Long firmId = seedFirm(unique("Acme"));
		applyUpdateCurrent("FIRM", firmId, json(Map.of("firmName", unique("Acme2"))));

		String body = mvc.perform(get("/api/firms/{id}/history", firmId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).contains("validTo");
	}
}
