package com.dcim.workflow;

import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/change-specs")
class ChangeSpecController {

	private final ChangeSpecService specs;

	ChangeSpecController(ChangeSpecService specs) {
		this.specs = specs;
	}

	@PostMapping
	ChangeSpecDto create(@RequestBody CreateSpecRequest request) {
		return wrap(() -> specs.create(request.ownerFirmId(), request.name(), request.actor()));
	}

	@GetMapping
	List<ChangeSpecDto> list(@RequestParam(required = false) Long ownerFirmId) {
		if (ownerFirmId == null) {
			return specs.listAll();
		}
		return specs.listForFirm(ownerFirmId);
	}

	@GetMapping("/{changeSpecId}")
	ChangeSpecDto get(@PathVariable Long changeSpecId) {
		return specs.find(changeSpecId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Change Spec not found: " + changeSpecId));
	}

	@PostMapping("/{changeSpecId}/changes/{changeId}")
	ChangeSpecDto addChange(@PathVariable Long changeSpecId, @PathVariable Long changeId) {
		return wrap(() -> specs.addChange(changeSpecId, changeId));
	}

	@DeleteMapping("/{changeSpecId}/changes/{changeId}")
	ChangeSpecDto removeChange(@PathVariable Long changeSpecId, @PathVariable Long changeId) {
		return wrap(() -> specs.removeChange(changeSpecId, changeId));
	}

	@PostMapping("/{changeSpecId}/chrecs")
	ChangeSpecDto linkChrec(@PathVariable Long changeSpecId, @RequestBody LinkChrecRequest request) {
		return wrap(() -> specs.linkChrec(changeSpecId, request.jiraKey(), request.title(), request.url()));
	}

	@DeleteMapping("/{changeSpecId}/chrecs/{chrecId}")
	ChangeSpecDto unlinkChrec(@PathVariable Long changeSpecId, @PathVariable Long chrecId) {
		return wrap(() -> specs.unlinkChrec(changeSpecId, chrecId));
	}

	@PostMapping("/{changeSpecId}/pending-billing")
	ChangeSpecDto pendingBilling(@PathVariable Long changeSpecId) {
		return wrap(() -> specs.submitPendingBilling(changeSpecId));
	}

	@PostMapping("/{changeSpecId}/apply")
	ChangeSpecDto apply(@PathVariable Long changeSpecId, @RequestBody ApplySpecRequest request) {
		return wrap(() -> specs.apply(changeSpecId, request.appliedBy()));
	}

	@GetMapping("/{changeSpecId}/validate")
	ChangeValidationResult validate(@PathVariable Long changeSpecId) {
		return wrap(() -> specs.validate(changeSpecId));
	}

	@PostMapping("/{changeSpecId}/cancel")
	ChangeSpecDto cancel(@PathVariable Long changeSpecId) {
		return wrap(() -> specs.cancel(changeSpecId));
	}

	private <T> T wrap(Callable<T> action) {
		try {
			return action.call();
		}
		catch (ValidationFailedException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
		}
		catch (WorkflowException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
		catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	record CreateSpecRequest(Long ownerFirmId, String name, String actor) {
	}

	record LinkChrecRequest(String jiraKey, String title, String url) {
	}

	record ApplySpecRequest(Long appliedBy) {
	}
}
