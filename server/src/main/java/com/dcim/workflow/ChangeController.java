package com.dcim.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/changes")
class ChangeController {

	private final ChangeService changes;

	ChangeController(ChangeService changes) {
		this.changes = changes;
	}

	@PostMapping
	ChangeDto create(@RequestBody CreateUntrackedRequest request) {
		return wrap(() -> changes.createUntracked(request.body(), request.actor()));
	}

	@GetMapping("/{changeId}")
	ChangeDto get(@PathVariable Long changeId) {
		return changes.find(changeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change not found: " + changeId));
	}

	@PutMapping("/{changeId}/payload")
	ChangeDto amend(@PathVariable Long changeId, @RequestBody AmendPayloadRequest request) {
		return wrap(() -> changes.amendPayload(changeId, request.body(), request.actor()));
	}

	@PostMapping("/{changeId}/stage")
	ChangeDto stage(@PathVariable Long changeId, @RequestBody StageRequest request) {
		return wrap(() -> changes.promoteToStaged(
				changeId,
				request.assetType(),
				request.action(),
				request.assetIdentityId(),
				request.baseHistoryId(),
				request.body(),
				request.actor()));
	}

	@PostMapping("/{changeId}/apply")
	ChangeDto apply(@PathVariable Long changeId, @RequestBody ApplyRequest request) {
		return wrap(() -> changes.applyStaged(changeId, request.appliedBy()));
	}

	@DeleteMapping("/{changeId}")
	void cancel(@PathVariable Long changeId) {
		wrap(() -> {
			changes.cancelOpen(changeId);
			return null;
		});
	}

	private <T> T wrap(java.util.concurrent.Callable<T> action) {
		try {
			return action.call();
		}
		catch (WorkflowException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
		catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	record CreateUntrackedRequest(String body, String actor) {
	}

	record AmendPayloadRequest(String body, String actor) {
	}

	record StageRequest(
			AssetType assetType,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			String body,
			String actor) {
	}

	record ApplyRequest(Long appliedBy) {
	}
}
