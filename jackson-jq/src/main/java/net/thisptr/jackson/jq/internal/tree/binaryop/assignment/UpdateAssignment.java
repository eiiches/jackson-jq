package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.Collections;
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
import com.fasterxml.jackson.databind.node.NullNode;

public class UpdateAssignment extends BinaryOperatorExpression {
	public UpdateAssignment(final JsonQuery lhs, final JsonQuery rhs) {
		super(lhs, rhs, "|=");
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		if (!(lhs instanceof FieldAccess))
			throw new IllegalJsonArgumentException("left hand side must be FieldAccess");

		final ResolvedPath resolvedPath = ((FieldAccess) lhs).resolvePath(scope, in);
		if (!(resolvedPath.target instanceof ThisObject))
			throw new IllegalJsonArgumentException("cannot update value");

		return Collections.singletonList((JsonNodeUtils.mutate(scope.getObjectMapper(), in, resolvedPath.path, new Mutation() {
			public JsonNode apply(final JsonNode node) throws JsonQueryException {
				final List<JsonNode> rvalues = rhs.apply(scope, node);
				if (rvalues.isEmpty())
					return NullNode.getInstance();
				return rvalues.get(rvalues.size() - 1);
			}
		}, true)));
	}
}
