package com.dcim;

import static java.util.Locale.ROOT;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;

public class UpperSnakeCaseNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {

	@Override
	protected Identifier unquotedIdentifier(Identifier name) {
		Identifier snakeCase = super.unquotedIdentifier(name);
		return Identifier.toIdentifier(snakeCase.getText().toUpperCase(ROOT), false);
	}
}
