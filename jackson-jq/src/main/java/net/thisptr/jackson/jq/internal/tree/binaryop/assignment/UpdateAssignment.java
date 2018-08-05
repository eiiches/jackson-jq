package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils.Mutation;
import net.thisptr.jackson.jq.internal.misc.JsonQueryUtils;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess.ResolvedPath;

public class UpdateAssignment extends BinaryOperatorExpression {
	public UpdateAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "|=");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		if (!(lhs instanceof FieldAccess))
			throw new IllegalJsonArgumentException("left hand side must be FieldAccess");

		final ResolvedPath resolvedPath = ((FieldAccess) lhs).resolvePath(scope, in);
		if (!(resolvedPath.target instanceof ThisObject))
			throw new IllegalJsonArgumentException("cannot update value");

		output.emit(JsonNodeUtils.mutate(scope.getObjectMapper(), in, resolvedPath.path, new Mutation() {
			@Override
			public JsonNode apply(final JsonNode node) throws JsonQueryException {
				final List<JsonNode> rvalues = JsonQueryUtils.applyToArrayList(rhs, scope, node);
				if (rvalues.isEmpty())
					return NullNode.getInstance();
				return rvalues.get(rvalues.size() - 1);
			}
		}, true));
	}
}
