package com.dcim.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dcim.workflow.validation.ValidationTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ChangeSpecApiTests extends ValidationTestSupport {

	@Autowired
	MockMvc mvc;

	@Test
	void createListValidateApplyAndCancelViaHttp() throws Exception {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");

		MvcResult created = mvc.perform(post("/api/change-specs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ownerFirmId\":" + ownerFirmId
								+ ",\"name\":\"" + unique("Spec") + "\",\"actor\":\"tester\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andReturn();
		long specId = objectMapper.readTree(created.getResponse().getContentAsString())
				.get("changeSpecId")
				.asLong();

		mvc.perform(get("/api/change-specs").param("ownerFirmId", ownerFirmId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.changeSpecId == %d)]", specId).exists());

		mvc.perform(get("/api/change-specs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.changeSpecId == %d)].ownerFirmName", specId).exists());

		mvc.perform(post("/api/change-specs/{id}/changes/{changeId}", specId, staged.changeId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.changeIds[0]").value(staged.changeId()));

		mvc.perform(post("/api/change-specs/{id}/chrecs", specId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jiraKey\":\"" + unique("CHREC")
								+ "\",\"title\":\"t\",\"url\":\"https://jira.example/1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chrecs.length()").value(1));

		mvc.perform(post("/api/change-specs/{id}/pending-billing", specId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_BILLING"));

		mvc.perform(get("/api/change-specs/{id}/validate", specId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issues").isEmpty());

		mvc.perform(post("/api/change-specs/{id}/apply", specId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"appliedBy\":" + appliedBy + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPLIED"));
	}

	@Test
	void cancelViaHttpAndRejectFurtherMutation() throws Exception {
		Long ownerFirmId = seedFirm(unique("Owner"));
		MvcResult created = mvc.perform(post("/api/change-specs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ownerFirmId\":" + ownerFirmId
								+ ",\"name\":\"" + unique("Spec") + "\",\"actor\":\"tester\"}"))
				.andExpect(status().isOk())
				.andReturn();
		long specId = objectMapper.readTree(created.getResponse().getContentAsString())
				.get("changeSpecId")
				.asLong();

		mvc.perform(post("/api/change-specs/{id}/cancel", specId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		mvc.perform(post("/api/change-specs/{id}/changes/{changeId}", specId, staged.changeId()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void removeChangeViaHttp() throws Exception {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), staged.changeId());

		mvc.perform(delete("/api/change-specs/{id}/changes/{changeId}", spec.changeSpecId(), staged.changeId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.changeIds").isEmpty());
	}

	@Test
	void missingSpecReturns404() throws Exception {
		mvc.perform(get("/api/change-specs/{id}", 999_999_999L))
				.andExpect(status().isNotFound());
	}
}
