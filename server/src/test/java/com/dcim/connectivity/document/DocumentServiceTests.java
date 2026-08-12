package com.dcim.connectivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class DocumentServiceTests extends ChangeTestSupport {

	@Test
	void addsDocumentThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		ChangeDto applied = applyAdd(
				"DOCUMENT",
				json(Map.of("documentName", "LOA-1", "crossConnectId", crossConnectId)));

		DocumentDto current = documents.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.documentName()).isEqualTo("LOA-1");
		assertThat(current.crossConnectId()).isEqualTo(crossConnectId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(documents.listCurrentByCrossConnect(crossConnectId)).hasSize(1);
		assertThat(documents.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesDocumentThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT1"), deps);
		Long otherCrossConnectId = seedCrossConnect(unique("CKT2"), deps);
		ChangeDto added = applyAdd(
				"DOCUMENT",
				json(Map.of("documentName", "LOA-1", "crossConnectId", crossConnectId)));

		applyUpdateCurrent(
				"DOCUMENT",
				added.assetIdentityId(),
				json(Map.of("documentName", "LOA-1B", "crossConnectId", otherCrossConnectId)));

		DocumentDto current = documents.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.documentName()).isEqualTo("LOA-1B");
		assertThat(current.crossConnectId()).isEqualTo(otherCrossConnectId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(documents.listCurrentByCrossConnect(crossConnectId)).isEmpty();
		assertThat(documents.listCurrentByCrossConnect(otherCrossConnectId)).hasSize(1);
		assertThat(documents.history(added.assetIdentityId())).hasSize(2);
		assertThat(documents.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesDocumentThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		ChangeDto added = applyAdd(
				"DOCUMENT",
				json(Map.of("documentName", "LOA-1", "crossConnectId", crossConnectId)));

		applyTerminateCurrent("DOCUMENT", added.assetIdentityId());

		DocumentDto current = documents.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.documentName()).isEqualTo("LOA-1");
		assertThat(documents.history(added.assetIdentityId())).hasSize(2);
		assertThat(documents.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}
}
