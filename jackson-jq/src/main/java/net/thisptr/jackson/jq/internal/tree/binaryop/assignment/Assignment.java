package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils.Mutation;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess.ResolvedPath;

import com.fasterxml.jackson.databind.JsonNode;

public class Assignment extends BinaryOperatorExpression {
	public Assignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, "=");
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		if (!(lhs instanceof FieldAccess))
			throw new IllegalJsonArgumentException("left hand side must be FieldAccess");

		final ResolvedPath resolvedPath = ((FieldAccess) lhs).resolvePath(scope, in);
		if (!(resolvedPath.target instanceof ThisObject))
			throw new IllegalJsonArgumentException("cannot update value");

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode rvalue : rhs.apply(scope, in))
			out.add(JsonNodeUtils.mutate(scope.getObjectMapper(), in, resolvedPath.path, new Mutation() {
				public JsonNode apply(final JsonNode node) {
					return rvalue;
				}
			}, true));
		return out;
	}
}
