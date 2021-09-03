package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class ArrayConstruction implements Expression {

	private Expression q;

	public ArrayConstruction() {
		this(null);
	}

	public ArrayConstruction(final Expression q) {
		this.q = q;
	}

	public Expression getQ() {
		return q;
	}

	public void setQ(Expression q) {
		this.q = q;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final ArrayNode array = new ArrayNode(scope.getObjectMapper().getNodeFactory());
		if (q != null)
			q.apply(scope, in, array::add);
		output.emit(array, null);
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
