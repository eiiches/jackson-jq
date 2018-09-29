package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.path.RootPath;

public class UpdateAssignment extends BinaryOperatorExpression {
	public UpdateAssignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "|=");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final JsonNode[] out = new JsonNode[] { in };

		lhs.apply(scope, in, RootPath.getInstance(), (lval, lpath) -> {
			if (lpath == null)
				throw new JsonQueryException("Invalid path expression with result " + lval); // FIXME: format
			final List<JsonNode> rvals = new ArrayList<>();
			rhs.apply(scope, lval, rvals::add);
			if (out[0] == null || rvals.isEmpty()) {
				out[0] = null;
			} else {
				out[0] = lpath.mutate(out[0], (lval_) -> rvals.get(rvals.size() - 1));
			}
		}, true);

		output.emit(out[0] != null ? out[0] : NullNode.getInstance());
	}
}
