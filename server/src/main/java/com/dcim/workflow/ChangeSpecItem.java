package com.dcim.workflow;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "T_CHANGE_SPEC_ITEM")
@IdClass(ChangeSpecItem.Pk.class)
public class ChangeSpecItem {

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_SPEC_ID", nullable = false)
	private ChangeSpec changeSpec;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_ID", nullable = false)
	private ChangeIdentity changeIdentity;

	protected ChangeSpecItem() {
	}

	public ChangeSpecItem(ChangeSpec changeSpec, ChangeIdentity changeIdentity) {
		this.changeSpec = changeSpec;
		this.changeIdentity = changeIdentity;
	}

	public ChangeSpec getChangeSpec() {
		return changeSpec;
	}

	public ChangeIdentity getChangeIdentity() {
		return changeIdentity;
	}

	public static final class Pk implements Serializable {

		private Long changeSpec;
		private Long changeIdentity;

		public Pk() {
		}

		public Pk(Long changeSpec, Long changeIdentity) {
			this.changeSpec = changeSpec;
			this.changeIdentity = changeIdentity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return Objects.equals(changeSpec, pk.changeSpec) && Objects.equals(changeIdentity, pk.changeIdentity);
		}

		@Override
		public int hashCode() {
			return Objects.hash(changeSpec, changeIdentity);
		}
	}
}
