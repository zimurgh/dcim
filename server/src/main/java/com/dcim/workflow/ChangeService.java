package com.dcim.workflow;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetChangeValidator;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.workflow.assettype.AssetTypeIdentity;
import com.dcim.workflow.assettype.AssetTypeService;

import jakarta.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeService {

	private final ChangeIdentityRepository identities;
	private final ChangePayloadRepository payloads;
	private final ChangeUntrackedRepository untracked;
	private final ChangeStagedRepository staged;
	private final ChangeCommittedRepository committed;
	private final ChangeCommittedHistoryRepository committedHistory;
	private final ChangeActionStatusRepository actionStatuses;
	private final ChangeSpecItemRepository specItems;
	private final ChangeViewRepository changeViews;
	private final AssetTypeService assetTypes;
	private final List<AssetChangeApplier> appliers;
	private final List<AssetChangeValidator> validators;
	private final EntityManager entityManager;
	private final Clock clock;

	ChangeService(
			ChangeIdentityRepository identities,
			ChangePayloadRepository payloads,
			ChangeUntrackedRepository untracked,
			ChangeStagedRepository staged,
			ChangeCommittedRepository committed,
			ChangeCommittedHistoryRepository committedHistory,
			ChangeActionStatusRepository actionStatuses,
			ChangeSpecItemRepository specItems,
			ChangeViewRepository changeViews,
			AssetTypeService assetTypes,
			List<AssetChangeApplier> appliers,
			List<AssetChangeValidator> validators,
			EntityManager entityManager,
			Optional<Clock> clock) {
		this.identities = identities;
		this.payloads = payloads;
		this.untracked = untracked;
		this.staged = staged;
		this.committed = committed;
		this.committedHistory = committedHistory;
		this.actionStatuses = actionStatuses;
		this.specItems = specItems;
		this.changeViews = changeViews;
		this.assetTypes = assetTypes;
		this.appliers = List.copyOf(appliers);
		this.validators = List.copyOf(validators);
		this.entityManager = entityManager;
		this.clock = clock.orElse(Clock.systemUTC());
	}

	@Transactional
	public ChangeDto createUntracked(String body, String actor) {
		Instant now = Instant.now(clock);
		ChangeIdentity identity = identities.saveAndFlush(new ChangeIdentity());
		ChangePayload payload = payloads.saveAndFlush(new ChangePayload(identity, requireBody(body), now));
		ChangeUntracked row = new ChangeUntracked(identity, payload, now, actor);
		entityManager.persist(row);
		entityManager.flush();
		return ChangeDto.untracked(row, statusLabel(ChangeAction.ADD, ChangeStage.UNTRACKED));
	}

	@Transactional
	public ChangeDto amendPayload(Long changeId, String body, String actor) {
		Instant now = Instant.now(clock);
		String nextBody = requireBody(body);

		Optional<ChangeUntracked> openUntracked = untracked.findById(changeId);
		if (openUntracked.isPresent()) {
			ChangeUntracked row = openUntracked.get();
			ChangePayload payload = payloads.save(new ChangePayload(row.getChangeIdentity(), nextBody, now));
			row.setPayload(payload);
			return ChangeDto.untracked(row, statusLabel(ChangeAction.ADD, ChangeStage.UNTRACKED));
		}

		ChangeStaged row = staged.findById(changeId)
				.orElseThrow(() -> new WorkflowException("No open change to amend: " + changeId));
		ChangePayload payload = payloads.save(new ChangePayload(row.getChangeIdentity(), nextBody, now));
		row.setPayload(payload);
		return toStagedDto(row);
	}

	@Transactional
	public ChangeDto promoteToStaged(
			Long changeId,
			String assetTypeCode,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			String body,
			String actor) {
		ChangeUntracked open = untracked.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not untracked: " + changeId));
		if (action == null) {
			throw new WorkflowException("Staging requires assetType and action");
		}
		AssetTypeIdentity assetType = assetTypes.requireIdentity(assetTypeCode);
		if (action != ChangeAction.ADD && (assetIdentityId == null || baseHistoryId == null)) {
			throw new WorkflowException("Update/Terminate staging requires assetIdentityId and baseHistoryId");
		}

		Instant now = Instant.now(clock);
		ChangeIdentity identity = open.getChangeIdentity();
		ChangePayload payload = open.getPayload();
		if (body != null && !body.isBlank()) {
			payload = payloads.save(new ChangePayload(identity, body, now));
		}

		untracked.delete(open);
		entityManager.flush();
		ChangeStaged row = new ChangeStaged(
				identity,
				payload,
				assetType,
				action,
				assetIdentityId,
				baseHistoryId,
				now,
				actor);
		entityManager.persist(row);
		return toStagedDto(row);
	}

	@Transactional
	public void cancelOpen(Long changeId) {
		if (committed.existsById(changeId)) {
			throw new WorkflowException("Committed change cannot be cancelled: " + changeId);
		}
		if (specItems.existsByChangeIdentity_ChangeId(changeId)) {
			throw new WorkflowException("Remove change from its Change Spec before cancelling: " + changeId);
		}
		if (untracked.existsById(changeId)) {
			untracked.deleteById(changeId);
			return;
		}
		if (staged.existsById(changeId)) {
			staged.deleteById(changeId);
			return;
		}
		throw new WorkflowException("No open change to cancel: " + changeId);
	}

	@Transactional(readOnly = true)
	public ChangeValidationResult validateStaged(Long changeId) {
		ChangeStaged open = requireStaged(changeId);
		ValidationContext context = batchContextFor(changeId);
		return new ChangeValidationResult(changeId, validate(open, context));
	}

	@Transactional(readOnly = true)
	List<ValidationIssue> validateAll(List<ChangeStaged> membership) {
		ValidationContext context = batchContext(membership);
		List<ValidationIssue> issues = new ArrayList<>();
		for (ChangeStaged open : membership) {
			for (ValidationIssue issue : validate(open, context)) {
				issues.add(new ValidationIssue(
						issue.code(),
						issue.field(),
						"Change " + open.getChangeId() + ": " + issue.message(),
						issue.relatedIdentityIds()));
			}
		}
		return List.copyOf(issues);
	}

	@Transactional
	public ChangeDto applyStaged(Long changeId, Long appliedBy) {
		ChangeStaged open = staged.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not staged: " + changeId));
		List<ValidationIssue> issues = validate(open, batchContextFor(changeId));
		if (!issues.isEmpty()) {
			throw new ValidationFailedException(issues);
		}
		return commitStaged(open, appliedBy);
	}

	@Transactional
	ChangeDto commitStaged(ChangeStaged open, Long appliedBy) {
		Instant now = Instant.now(clock);
		LocalDate validOn = LocalDate.ofInstant(now, ZoneOffset.UTC);
		String committedStatus = statusLabel(open.getAction(), ChangeStage.COMMITTED);
		String assetTypeCode = assetTypes.requireCode(open.getAssetType());

		AssetApplyResult applied;
		try {
			applied = applierFor(assetTypeCode).apply(new AssetApplyCommand(
					assetTypeCode,
					open.getAction().name(),
					open.getPayload().getBody(),
					open.getAssetIdentityId(),
					open.getBaseHistoryId(),
					now,
					validOn,
					appliedBy,
					committedStatus));
		}
		catch (AssetApplyException ex) {
			throw new WorkflowException(ex.getMessage(), ex);
		}

		ChangeCommitted row = new ChangeCommitted(
				open.getChangeIdentity(),
				open.getPayload(),
				open.getAssetType(),
				open.getAction(),
				now,
				appliedBy);
		staged.delete(open);
		entityManager.flush();
		entityManager.persist(row);

		List<ChangeDto.HistoryLinkDto> links = applied.links().stream()
				.map(link -> {
					HistoryLinkRole role = toRole(link.role());
					committedHistory.save(new ChangeCommittedHistory(
							row,
							open.getAssetType(),
							link.historyId(),
							role));
					return new ChangeDto.HistoryLinkDto(
							assetTypeCode,
							open.getAssetType().getAssetTypeId(),
							link.historyId(),
							role);
				})
				.toList();

		return ChangeDto.committed(row, assetTypeCode, committedStatus, applied.assetIdentityId(), links);
	}

	@Transactional(readOnly = true)
	public List<ChangeDto> listAll() {
		return changeViews.findAllLatestFirst().stream().map(ChangeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<ChangeDto> find(Long changeId) {
		Optional<ChangeUntracked> openUntracked = untracked.findById(changeId);
		if (openUntracked.isPresent()) {
			return Optional.of(ChangeDto.untracked(
					openUntracked.get(),
					statusLabel(ChangeAction.ADD, ChangeStage.UNTRACKED)));
		}
		Optional<ChangeStaged> openStaged = staged.findById(changeId);
		if (openStaged.isPresent()) {
			return Optional.of(toStagedDto(openStaged.get()));
		}
		return committed.findById(changeId).map(row -> {
			String assetTypeCode = assetTypes.requireCode(row.getAssetType());
			return ChangeDto.committed(
					row,
					assetTypeCode,
					statusLabel(row.getAction(), ChangeStage.COMMITTED),
					null,
					committedHistory.findByCommitted_ChangeId(changeId).stream()
							.map(link -> new ChangeDto.HistoryLinkDto(
									assetTypes.requireCode(link.getAssetType()),
									link.getAssetType().getAssetTypeId(),
									link.getHistoryId(),
									link.getRole()))
							.toList());
		});
	}

	ChangeStaged requireStaged(Long changeId) {
		return staged.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not staged: " + changeId));
	}

	List<ChangeStaged> loadStaged(List<Long> changeIds) {
		List<ChangeStaged> rows = new ArrayList<>();
		for (Long changeId : changeIds) {
			rows.add(requireStaged(changeId));
		}
		return rows;
	}

	private ChangeDto toStagedDto(ChangeStaged row) {
		return ChangeDto.staged(
				row,
				assetTypes.requireCode(row.getAssetType()),
				statusLabel(row.getAction(), ChangeStage.STAGED));
	}

	private List<ValidationIssue> validate(ChangeStaged open, ValidationContext context) {
		String assetTypeCode = assetTypes.requireCode(open.getAssetType());
		AssetValidateCommand command = new AssetValidateCommand(
				assetTypeCode,
				open.getAction().name(),
				open.getPayload().getBody(),
				open.getAssetIdentityId(),
				open.getBaseHistoryId());
		return validatorFor(assetTypeCode).validate(command, context);
	}

	private ValidationContext batchContextFor(Long changeId) {
		List<ChangeSpecItem> membership = specItems.findByChangeIdentity_ChangeId(changeId);
		if (membership.isEmpty()) {
			ChangeStaged open = requireStaged(changeId);
			return batchContext(List.of(open));
		}
		Long changeSpecId = membership.getFirst().getChangeSpec().getChangeSpecId();
		List<Long> changeIds = specItems.findByChangeSpec_ChangeSpecId(changeSpecId).stream()
				.map(item -> item.getChangeIdentity().getChangeId())
				.toList();
		return batchContext(loadStaged(changeIds));
	}

	private ValidationContext batchContext(List<ChangeStaged> membership) {
		List<ValidationContext.BatchIntent> intents = membership.stream()
				.map(row -> new ValidationContext.BatchIntent(
						assetTypes.requireCode(row.getAssetType()),
						row.getAction().name(),
						row.getAssetIdentityId()))
				.toList();
		return new ValidationContext(intents);
	}

	private AssetChangeApplier applierFor(String assetTypeCode) {
		return appliers.stream()
				.filter(applier -> applier.supports(assetTypeCode))
				.findFirst()
				.orElseThrow(() -> new WorkflowException("No asset applier for " + assetTypeCode));
	}

	private AssetChangeValidator validatorFor(String assetTypeCode) {
		return validators.stream()
				.filter(validator -> validator.supports(assetTypeCode))
				.findFirst()
				.orElseThrow(() -> new WorkflowException("No asset validator for " + assetTypeCode));
	}

	private static HistoryLinkRole toRole(String role) {
		if (AssetHistoryLink.ROLE_CREATED.equals(role)) {
			return HistoryLinkRole.CREATED;
		}
		if (AssetHistoryLink.ROLE_CLOSED_PRIOR.equals(role)) {
			return HistoryLinkRole.CLOSED_PRIOR;
		}
		throw new WorkflowException("Unknown history link role: " + role);
	}

	private String statusLabel(ChangeAction action, ChangeStage stage) {
		return actionStatuses.findByActionAndStage(action, stage)
				.map(ChangeActionStatus::getStatus)
				.orElse(stage.name());
	}

	private static String requireBody(String body) {
		if (body == null || body.isBlank()) {
			throw new WorkflowException("Change payload body is required");
		}
		return body;
	}
}
