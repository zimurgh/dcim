package com.dcim.workflow;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_CHANGE_ACTION_STATUS")
@IdClass(ChangeActionStatus.Pk.class)
public class ChangeActionStatus {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "ACTION", nullable = false, length = 50)
	private ChangeAction action;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "STAGE", nullable = false, length = 50)
	private ChangeStage stage;

	@Column(name = "STATUS", nullable = false, length = 50)
	private String status;

	protected ChangeActionStatus() {
	}

	public ChangeAction getAction() {
		return action;
	}

	public ChangeStage getStage() {
		return stage;
	}

	public String getStatus() {
		return status;
	}

	public static final class Pk implements Serializable {

		private ChangeAction action;
		private ChangeStage stage;

		public Pk() {
		}

		public Pk(ChangeAction action, ChangeStage stage) {
			this.action = action;
			this.stage = stage;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return action == pk.action && stage == pk.stage;
		}

		@Override
		public int hashCode() {
			return Objects.hash(action, stage);
		}
	}
}
