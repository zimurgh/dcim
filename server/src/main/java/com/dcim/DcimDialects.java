package com.dcim;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.type.SqlTypes;

/**
 * Maps Java {@link Long} IDs to {@code INT UNSIGNED} so schema validation matches the ledger.
 */
public final class DcimDialects {

	private DcimDialects() {
	}

	public static final class MariaDB extends MariaDBDialect {
		@Override
		protected String columnType(int sqlTypeCode) {
			if (sqlTypeCode == SqlTypes.BIGINT) {
				return "int unsigned";
			}
			return super.columnType(sqlTypeCode);
		}
	}

	public static final class H2 extends H2Dialect {
		@Override
		protected String columnType(int sqlTypeCode) {
			if (sqlTypeCode == SqlTypes.BIGINT) {
				return "integer";
			}
			return super.columnType(sqlTypeCode);
		}
	}
}
