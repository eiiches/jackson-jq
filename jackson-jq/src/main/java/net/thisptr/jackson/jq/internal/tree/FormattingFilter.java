package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class FormattingFilter implements Expression {
	private String name;
	private Version version;

	public FormattingFilter(final String name, final Version version) {
		this.name = name;
		this.version = version;
	}

	public FormattingFilter() {}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final Function f = scope.getFunction("@" + name, 0);
		if (f == null)
			throw new JsonQueryException("Formatting operator @" + name + " does not exist");
		f.apply(scope, Collections.emptyList(), in, ipath, output, version);
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
