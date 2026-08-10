package com.dcim.workflow;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_CHANGE_SPEC_CHREC")
@IdClass(ChangeSpecChrec.Pk.class)
public class ChangeSpecChrec {

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHANGE_SPEC_ID", nullable = false)
	private ChangeSpec changeSpec;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CHREC_ID", nullable = false)
	private Chrec chrec;

	protected ChangeSpecChrec() {
	}

	public ChangeSpecChrec(ChangeSpec changeSpec, Chrec chrec) {
		this.changeSpec = changeSpec;
		this.chrec = chrec;
	}

	public ChangeSpec getChangeSpec() {
		return changeSpec;
	}

	public Chrec getChrec() {
		return chrec;
	}

	public static final class Pk implements Serializable {

		private Long changeSpec;
		private Long chrec;

		public Pk() {
		}

		public Pk(Long changeSpec, Long chrec) {
			this.changeSpec = changeSpec;
			this.chrec = chrec;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return Objects.equals(changeSpec, pk.changeSpec) && Objects.equals(chrec, pk.chrec);
		}

		@Override
		public int hashCode() {
			return Objects.hash(changeSpec, chrec);
		}
	}
}
