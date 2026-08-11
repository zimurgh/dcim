package com.dcim.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ChangeApiTests extends ValidationTestSupport {

	@Autowired
	MockMvc mvc;

	@Test
	void createStageValidateApplyViaHttp() throws Exception {
		String name = unique("NY");
		MvcResult created = mvc.perform(post("/api/changes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonBody(name)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("UNTRACKED"))
				.andReturn();
		long changeId = readChangeId(created);

		mvc.perform(put("/api/changes/{id}/payload", changeId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonBody(name + "-B")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString(name + "-B")));

		mvc.perform(post("/api/changes/{id}/stage", changeId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"assetType":"DATA_CENTER","action":"ADD","actor":"tester"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("STAGED"));

		mvc.perform(get("/api/changes/{id}/validate", changeId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issues").isEmpty());

		mvc.perform(post("/api/changes/{id}/apply", changeId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"appliedBy\":" + appliedBy + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("COMMITTED"));
	}

	@Test
	void applyConflictWhenInvalid() throws Exception {
		MvcResult created = mvc.perform(post("/api/changes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"{}\",\"actor\":\"tester\"}"))
				.andExpect(status().isOk())
				.andReturn();
		long changeId = readChangeId(created);

		mvc.perform(post("/api/changes/{id}/stage", changeId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"assetType":"DATA_CENTER","action":"ADD","actor":"tester"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(post("/api/changes/{id}/apply", changeId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"appliedBy\":" + appliedBy + "}"))
				.andExpect(status().isConflict());
	}

	@Test
	void cancelOpenViaHttp() throws Exception {
		MvcResult created = mvc.perform(post("/api/changes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonBody("X")))
				.andExpect(status().isOk())
				.andReturn();
		long changeId = readChangeId(created);

		mvc.perform(delete("/api/changes/{id}", changeId))
				.andExpect(status().isOk());
		mvc.perform(get("/api/changes/{id}", changeId))
				.andExpect(status().isNotFound());
	}

	@Test
	void missingChangeReturns404() throws Exception {
		mvc.perform(get("/api/changes/{id}", 999_999_999L))
				.andExpect(status().isNotFound());
	}

	private String jsonBody(String dataCenterName) {
		return json(java.util.Map.of(
				"body", json(java.util.Map.of("dataCenterName", dataCenterName)),
				"actor", "tester"));
	}

	private long readChangeId(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("changeId").asLong();
	}
}
