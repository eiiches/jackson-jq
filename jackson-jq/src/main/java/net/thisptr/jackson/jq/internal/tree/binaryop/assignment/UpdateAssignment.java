package net.thisptr.jackson.jq.internal.tree.binaryop.assignment;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryUndefinedBehaviorException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.path.Path;
import net.thisptr.jackson.jq.path.RootPath;

public class UpdateAssignment extends BinaryOperatorExpression {
	private Version version;

	public UpdateAssignment(final Expression lhs, final Expression rhs, final Version version) {
		super(lhs, rhs, "|=");
		this.version = version;
	}

	public UpdateAssignment() {}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final JsonNode[] out = new JsonNode[] { in };
		lhs.apply(scope, in, RootPath.getInstance(), (lval, lpath) -> {
			// `VALUE | path(VALUE) => []`
			if (lpath == null && in.isValueNode() && JsonNodeComparator.getInstance().compare(in, lval) == 0)
				lpath = RootPath.getInstance();
			if (lpath == null)
				throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(lval));

			out[0] = lpath.mutate(out[0], (lval_) -> {
				final List<JsonNode> rvals = new ArrayList<>();
				rhs.apply(scope, lval_ == null ? NullNode.getInstance() : lval_, rvals::add);
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
