package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class AlternativeOperatorExpression extends BinaryOperatorExpression {
	public AlternativeOperatorExpression(final Expression valueExpr, final Expression defaultExpr) {
		super(valueExpr, defaultExpr, "//");
	}

	public AlternativeOperatorExpression() {
		super(null, null,  "//");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final AtomicBoolean emitted = new AtomicBoolean();
		lhs.apply(scope, in, path, (out, outpath) -> {
			if (JsonNodeUtils.asBoolean(out)) {
				output.emit(out, outpath);
				emitted.set(true);
			}
		}, requirePath);
		if (!emitted.get()) {
			rhs.apply(scope, in, path, output, requirePath);
		}
	}
}
