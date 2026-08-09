package com.dcim;

import static java.util.Locale.ROOT;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;

/**
 * Physical names are UPPER_SNAKE_CASE to match DCIM MariaDB conventions
 * (e.g. {@code T_FIRM_HISTORY}, {@code FIRM_NAME}).
 */
public class UpperSnakeCaseNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {

	@Override
	protected Identifier unquotedIdentifier(Identifier name) {
		Identifier snakeCase = super.unquotedIdentifier(name);
		return Identifier.toIdentifier(snakeCase.getText().toUpperCase(ROOT), false);
	}
}
