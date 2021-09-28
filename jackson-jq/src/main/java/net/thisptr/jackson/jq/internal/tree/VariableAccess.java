package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Scope.ValueWithPath;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class VariableAccess implements Expression {
	private final String name;
	private final String moduleName;

	public VariableAccess(final String moduleName, final String name) {
		this.moduleName = moduleName;
		this.name = name;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (moduleName != null) {
			JsonNode data = null;
			if (moduleName.equals(name))
				data = scope.getImportedData(name);
			if (data == null)
				throw new JsonQueryException(String.format("$%s::%s is not defined", moduleName, name));
			output.emit(data, null);
		} else {
			final ValueWithPath value = scope.getValueWithPath(name);
			if (value != null) {
				output.emit(value.value(), null);
				return;
			}

			final JsonNode data = scope.getImportedData(name);
			if (data != null) {
				output.emit(data, null);
				return;
			}

			throw new JsonQueryException(String.format("$%s is not defined", name));
		}
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append('$');
		if (moduleName != null) {
			s.append(moduleName);
			s.append("::");
		}
		s.append(name);
		return s.toString();
	}
}
