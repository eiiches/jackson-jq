package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.operators.BinaryOperator;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class ComplexAssignment extends BinaryOperatorExpression {
	private BinaryOperator operator;

	public ComplexAssignment(final Expression lhs, final Expression rhs, final BinaryOperator operator) {
		super(lhs, rhs, operator.image() + "=");
		this.operator = operator;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		rhs.apply(scope, in, (rval) -> {
			final List<Path> lpaths = new ArrayList<>();
			lhs.apply(scope, in, RootPath.getInstance(), (lval, lpath) -> {
				// `VALUE | path(VALUE) => []`
				if (lpath == null && in.isValueNode() && JsonNodeComparator.getInstance().compare(in, lval) == 0)
					lpath = RootPath.getInstance();
				if (lpath == null)
					throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(lval));
				lpaths.add(lpath);
			}, true);
			JsonNode out = in;
			for (final Path lpath : lpaths)
				out = lpath.mutate(out, (lval) -> operator.apply(scope.getObjectMapper(), lval == null ? NullNode.getInstance() : lval, rval));
			output.emit(out, null);
		});
	}
}
