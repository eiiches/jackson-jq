package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.exception.JsonQueryUndefinedBehaviorException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class UpdateAssignment<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private Version version;

	public UpdateAssignment(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs, Version version) {
		super(lhs, rhs, "|=");
		this.jsonProvider = jsonProvider;
		this.version = version;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Expression<JsonNode> typedLhs = lhs;
		Expression<JsonNode> typedRhs = rhs;
		JsonNode[] out = (JsonNode[]) new Object[] { in };
		typedLhs.apply(frame, in, RootPath.getInstance(), (lval, lpath0) -> {
			@Var Path<JsonNode> lpath = lpath0;
			// `VALUE | path(VALUE) => []`
			if (lpath == null && JsonNodeUtils.isValueNode(jsonProvider, in) && new JsonNodeComparator<>(jsonProvider).compare(in, lval) == 0)
				lpath = RootPath.getInstance();
			if (lpath == null)
				throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(jsonProvider, lval));

			out[0] = lpath.mutate(jsonProvider, out[0], (lval_) -> {
				List<JsonNode> rvals = new ArrayList<>();
				typedRhs.apply(frame, lval_ == null ? jsonProvider.createNull() : lval_, rvals::add);
				if (rvals.isEmpty())
					throw new JsonQueryUndefinedBehaviorException("`|= empty` is undefined. See https://github.com/stedolan/jq/issues/897");
				if (version.compareTo(Versions.JQ_1_6) >= 0) {
					return rvals.get(0);
				} else {
					return rvals.get(rvals.size() - 1);
				}
			});
		}, true);
		output.emit(out[0], null);
	}
}
