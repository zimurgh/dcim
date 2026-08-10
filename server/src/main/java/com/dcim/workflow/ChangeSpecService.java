package com.dcim.workflow;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.dcim.organization.firm.FirmIdentity;
import com.dcim.organization.firm.FirmIdentityRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeSpecService {

	private final ChangeSpecRepository specs;
	private final ChangeSpecItemRepository items;
	private final ChangeSpecChrecRepository specChrecs;
	private final ChrecRepository chrecs;
	private final FirmIdentityRepository firms;
	private final ChangeService changes;
	private final Clock clock;

	ChangeSpecService(
			ChangeSpecRepository specs,
			ChangeSpecItemRepository items,
			ChangeSpecChrecRepository specChrecs,
			ChrecRepository chrecs,
			FirmIdentityRepository firms,
			ChangeService changes,
			Optional<Clock> clock) {
		this.specs = specs;
		this.items = items;
		this.specChrecs = specChrecs;
		this.chrecs = chrecs;
		this.firms = firms;
		this.changes = changes;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	@Transactional
	public ChangeSpecDto create(Long ownerFirmId, String name, String actor) {
		FirmIdentity firm = firms.findById(ownerFirmId)
				.orElseThrow(() -> new WorkflowException("Owner firm not found: " + ownerFirmId));
		ChangeSpec spec = specs.save(new ChangeSpec(
				firm,
				name,
				ChangeSpecStatus.DRAFT,
				Instant.now(clock),
				actor));
		return toDto(spec);
	}

	@Transactional(readOnly = true)
	public Optional<ChangeSpecDto> find(Long changeSpecId) {
		return specs.findById(changeSpecId).map(this::toDto);
	}

	@Transactional(readOnly = true)
	public List<ChangeSpecDto> listForFirm(Long ownerFirmId) {
		return specs.findByOwnerFirm_FirmIdOrderByChangeSpecIdAsc(ownerFirmId).stream()
				.map(this::toDto)
				.toList();
	}

	@Transactional
	public ChangeSpecDto addChange(Long changeSpecId, Long changeId) {
		ChangeSpec spec = requireMutable(changeSpecId);
		ChangeStaged staged = changes.requireStaged(changeId);
		ChangeSpecItem.Pk pk = new ChangeSpecItem.Pk(changeSpecId, changeId);
		if (items.existsById(pk)) {
			throw new WorkflowException("Change already on spec: " + changeId);
		}
		items.save(new ChangeSpecItem(spec, staged.getChangeIdentity()));
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto removeChange(Long changeSpecId, Long changeId) {
		ChangeSpec spec = requireMutable(changeSpecId);
		items.deleteByChangeSpec_ChangeSpecIdAndChangeIdentity_ChangeId(changeSpecId, changeId);
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto linkChrec(Long changeSpecId, String jiraKey, String title, String url) {
		ChangeSpec spec = requireMutable(changeSpecId);
		if (jiraKey == null || jiraKey.isBlank()) {
			throw new WorkflowException("CHREC jiraKey is required");
		}
		Chrec chrec = chrecs.findByJiraKey(jiraKey)
				.orElseGet(() -> chrecs.save(new Chrec(jiraKey, title, url)));
		ChangeSpecChrec.Pk pk = new ChangeSpecChrec.Pk(changeSpecId, chrec.getChrecId());
		if (!specChrecs.existsById(pk)) {
			specChrecs.save(new ChangeSpecChrec(spec, chrec));
		}
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto unlinkChrec(Long changeSpecId, Long chrecId) {
		ChangeSpec spec = requireMutable(changeSpecId);
		if (spec.getStatus() != ChangeSpecStatus.DRAFT
				&& specChrecs.countByChangeSpec_ChangeSpecId(changeSpecId) <= 1
				&& specChrecs.existsById(new ChangeSpecChrec.Pk(changeSpecId, chrecId))) {
			throw new WorkflowException("At least one CHREC is required after Draft");
		}
		specChrecs.deleteByChangeSpec_ChangeSpecIdAndChrec_ChrecId(changeSpecId, chrecId);
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto submitPendingBilling(Long changeSpecId) {
		ChangeSpec spec = requireStatus(changeSpecId, ChangeSpecStatus.DRAFT);
		if (specChrecs.countByChangeSpec_ChangeSpecId(changeSpecId) < 1) {
			throw new WorkflowException("At least one CHREC is required to enter Pending Billing");
		}
		spec.setStatus(ChangeSpecStatus.PENDING_BILLING);
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto apply(Long changeSpecId, Long appliedBy) {
		ChangeSpec spec = requireStatus(changeSpecId, ChangeSpecStatus.PENDING_BILLING);
		if (specChrecs.countByChangeSpec_ChangeSpecId(changeSpecId) < 1) {
			throw new WorkflowException("At least one CHREC is required to apply");
		}
		List<ChangeSpecItem> membership = items.findByChangeSpec_ChangeSpecId(changeSpecId);
		for (ChangeSpecItem item : membership) {
			ChangeStaged staged = changes.requireStaged(item.getChangeIdentity().getChangeId());
			changes.commitStaged(staged, appliedBy);
		}
		spec.setStatus(ChangeSpecStatus.APPLIED);
		return toDto(spec);
	}

	@Transactional
	public ChangeSpecDto cancel(Long changeSpecId) {
		ChangeSpec spec = requireMutable(changeSpecId);
		spec.setStatus(ChangeSpecStatus.CANCELLED);
		return toDto(spec);
	}

	private ChangeSpec requireMutable(Long changeSpecId) {
		ChangeSpec spec = specs.findById(changeSpecId)
				.orElseThrow(() -> new WorkflowException("Change Spec not found: " + changeSpecId));
		if (spec.getStatus() == ChangeSpecStatus.APPLIED || spec.getStatus() == ChangeSpecStatus.CANCELLED) {
			throw new WorkflowException("Change Spec is not mutable: " + changeSpecId);
		}
		return spec;
	}

	private ChangeSpec requireStatus(Long changeSpecId, ChangeSpecStatus expected) {
		ChangeSpec spec = specs.findById(changeSpecId)
				.orElseThrow(() -> new WorkflowException("Change Spec not found: " + changeSpecId));
		if (spec.getStatus() != expected) {
			throw new WorkflowException(
					"Change Spec " + changeSpecId + " must be " + expected + " but is " + spec.getStatus());
		}
		return spec;
	}

	private ChangeSpecDto toDto(ChangeSpec spec) {
		List<Long> changeIds = items.findByChangeSpec_ChangeSpecId(spec.getChangeSpecId()).stream()
				.map(item -> item.getChangeIdentity().getChangeId())
				.toList();
		List<ChangeSpecDto.ChrecDto> linked = specChrecs.findByChangeSpec_ChangeSpecId(spec.getChangeSpecId()).stream()
				.map(link -> ChangeSpecDto.ChrecDto.from(link.getChrec()))
				.toList();
		return new ChangeSpecDto(
				spec.getChangeSpecId(),
				spec.getOwnerFirm().getFirmId(),
				spec.getName(),
				spec.getStatus(),
				spec.getCreatedAt(),
				spec.getCreatedBy(),
				changeIds,
				linked);
	}
}
