package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class Assignment extends BinaryOperatorExpression {
	public Assignment(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "=");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		rhs.apply(scope, in, (rval) -> {
			final List<Path> lpaths = new ArrayList<>();
			lhs.apply(scope, in, RootPath.getInstance(), (lval, lpath) -> {
				if (lpath == null)
					throw new JsonQueryException("Invalid path expression with result " + lval); // FIXME: format
				lpaths.add(lpath);
			}, true);
			JsonNode out = in;
			for (final Path lpath : lpaths)
				out = lpath.mutate(out, (lval_) -> rval);
			output.emit(out);
		});
	}
}
