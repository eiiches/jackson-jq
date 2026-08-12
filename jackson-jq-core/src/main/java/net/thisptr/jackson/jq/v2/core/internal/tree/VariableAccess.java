package net.thisptr.jackson.jq.v2.core.internal.tree;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Scope.ValueWithPath;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class VariableAccess<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final String moduleName;

	public VariableAccess(String moduleName, String name) {
		this.moduleName = moduleName;
		this.name = name;
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		if (moduleName != null) {
			@Var JsonNode data = null;
			if (moduleName.equals(name))
				data = scope.getImportedData(name);
			if (data == null)
				throw new JsonQueryException(String.format("$%s::%s is not defined", moduleName, name));
			output.emit(data, null);
		} else {
			ValueWithPath<JsonNode> value = scope.getValueWithPath(name);
			if (value != null) {
				output.emit(value.value(), null);
				return;
			}

			JsonNode data = scope.getImportedData(name);
			if (data != null) {
				output.emit(data, null);
				return;
			}

			throw new JsonQueryException(String.format("$%s is not defined", name));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append('$');
		if (moduleName != null) {
			s.append(moduleName);
			s.append("::");
		}
		s.append(name);
		return s.toString();
	}
}
