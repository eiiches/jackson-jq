package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class TryCatch extends JsonQuery {
	protected JsonQuery tryExpr;
	protected JsonQuery catchExpr;

	public TryCatch(final JsonQuery tryExpr, final JsonQuery catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(final JsonQuery tryExpr) {
		this(tryExpr, null);
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		try {
			return tryExpr.apply(scope, in);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				return catchExpr.apply(scope, new TextNode(e.getMessage()));
			} else {
				return Collections.emptyList();
			}
		}
	}

	public static class Question extends TryCatch {
		public Question(JsonQuery tryExpr) {
			super(tryExpr);
		}

		@Override
		public String toString() {
			return String.format("(%s)?", tryExpr);
		}
	}

	@Override
	public String toString() {
		if (catchExpr != null) {
			return String.format("(try (%s) catch (%s))", tryExpr, catchExpr);
		} else {
			return String.format("(try (%s))", tryExpr);
		}
	}
}
