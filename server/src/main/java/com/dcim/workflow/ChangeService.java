package com.dcim.workflow;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;

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
	private final List<AssetChangeApplier> appliers;
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
			List<AssetChangeApplier> appliers,
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
		this.appliers = List.copyOf(appliers);
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
		return ChangeDto.staged(row, statusLabel(row.getAction(), ChangeStage.STAGED));
	}

	@Transactional
	public ChangeDto promoteToStaged(
			Long changeId,
			AssetType assetType,
			ChangeAction action,
			Long assetIdentityId,
			Long baseHistoryId,
			String body,
			String actor) {
		ChangeUntracked open = untracked.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not untracked: " + changeId));
		if (assetType == null || action == null) {
			throw new WorkflowException("Staging requires assetType and action");
		}
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
		return ChangeDto.staged(row, statusLabel(action, ChangeStage.STAGED));
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

	@Transactional
	public ChangeDto applyStaged(Long changeId, Long appliedBy) {
		ChangeStaged open = staged.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not staged: " + changeId));
		return commitStaged(open, appliedBy);
	}

	@Transactional
	ChangeDto commitStaged(ChangeStaged open, Long appliedBy) {
		Instant now = Instant.now(clock);
		LocalDate validOn = LocalDate.ofInstant(now, ZoneOffset.UTC);
		String committedStatus = statusLabel(open.getAction(), ChangeStage.COMMITTED);

		AssetApplyResult applied;
		try {
			applied = applierFor(open.getAssetType()).apply(new AssetApplyCommand(
					open.getAssetType().name(),
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
					return new ChangeDto.HistoryLinkDto(open.getAssetType(), link.historyId(), role);
				})
				.toList();

		return ChangeDto.committed(row, committedStatus, applied.assetIdentityId(), links);
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
			ChangeStaged row = openStaged.get();
			return Optional.of(ChangeDto.staged(row, statusLabel(row.getAction(), ChangeStage.STAGED)));
		}
		return committed.findById(changeId).map(row -> ChangeDto.committed(
				row,
				statusLabel(row.getAction(), ChangeStage.COMMITTED),
				null,
				committedHistory.findByCommitted_ChangeId(changeId).stream()
						.map(link -> new ChangeDto.HistoryLinkDto(
								link.getAssetType(),
								link.getHistoryId(),
								link.getRole()))
						.toList()));
	}

	ChangeStaged requireStaged(Long changeId) {
		return staged.findById(changeId)
				.orElseThrow(() -> new WorkflowException("Change is not staged: " + changeId));
	}

	private AssetChangeApplier applierFor(AssetType assetType) {
		return appliers.stream()
				.filter(applier -> applier.supports(assetType.name()))
				.findFirst()
				.orElseThrow(() -> new WorkflowException("No asset applier for " + assetType));
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
