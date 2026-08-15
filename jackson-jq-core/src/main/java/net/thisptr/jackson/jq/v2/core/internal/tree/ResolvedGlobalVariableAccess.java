package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedGlobalVariableAccess<JsonNode> implements Expression {
	private final String name;
	private final Supplier<JsonNode> valueSupplier;

	public ResolvedGlobalVariableAccess(String name, Supplier<JsonNode> valueSupplier) {
		this.name = name;
		this.valueSupplier = valueSupplier;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		N val = (N) valueSupplier.get();
		if (val == null)
			throw new JsonQueryException(String.format("Variable $%s evaluated to null", name));
		output.emit(val, null);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
