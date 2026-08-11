package com.dcim.asset;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import tools.jackson.databind.JsonNode;

public abstract class AbstractAssetChangeApplier<I, H extends AuditHistory> implements AssetChangeApplier {

	private final String assetType;
	private final String assetLabel;
	private final JpaRepository<I, Long> identities;
	private final JpaRepository<H, Long> history;
	private final JsonPayloads payloads;
	private final Supplier<I> newIdentity;
	private final Function<I, Long> identityId;
	private final Function<H, Long> historyIdentityId;
	private final Function<H, Long> historyId;

	protected AbstractAssetChangeApplier(
			String assetType,
			String assetLabel,
			JpaRepository<I, Long> identities,
			JpaRepository<H, Long> history,
			JsonPayloads payloads,
			Supplier<I> newIdentity,
			Function<I, Long> identityId,
			Function<H, Long> historyIdentityId,
			Function<H, Long> historyId) {
		this.assetType = assetType;
		this.assetLabel = assetLabel;
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
		this.newIdentity = newIdentity;
		this.identityId = identityId;
		this.historyIdentityId = historyIdentityId;
		this.historyId = historyId;
	}

	@Override
	public final boolean supports(String assetType) {
		return this.assetType.equals(assetType);
	}

	@Override
	public final AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException(
					"Unsupported " + assetLabel + " action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		I identity = identities.saveAndFlush(newIdentity.get());
		H created = history.saveAndFlush(createAdd(identity, body, command));
		return AssetApplyLifecycle.created(identityId.apply(identity), historyId.apply(created));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		H prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		prior.close(command.validOn());
		H created = history.saveAndFlush(createUpdate(prior, body, command));
		return AssetApplyLifecycle.revised(
				historyIdentityId.apply(prior), historyId.apply(prior), historyId.apply(created));
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		H prior = requireCurrentBase(command);
		prior.close(command.validOn());
		H created = history.saveAndFlush(createTerminate(prior, command));
		return AssetApplyLifecycle.revised(
				historyIdentityId.apply(prior), historyId.apply(prior), historyId.apply(created));
	}

	private H requireCurrentBase(AssetApplyCommand command) {
		return AssetApplyLifecycle.requireCurrentBase(
				command, history::findById, historyIdentityId, assetLabel);
	}

	protected abstract H createAdd(I identity, JsonNode body, AssetApplyCommand command);

	protected abstract H createUpdate(H prior, JsonNode body, AssetApplyCommand command);

	protected abstract H createTerminate(H prior, AssetApplyCommand command);

	protected final JsonPayloads payloads() {
		return payloads;
	}
}
