package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class TryCatch extends JsonQuery {
	private JsonQuery tryExpr;
	private JsonQuery catchExpr;

	public TryCatch(final JsonQuery tryExpr, final JsonQuery catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		try {
			return tryExpr.apply(scope, in);
		} catch (JsonQueryException e) {
			return catchExpr.apply(scope, new TextNode(e.getMessage()));
		}
	}

	@Override
	public String toString() {
		return String.format("(try (%s) catch (%s))", tryExpr, catchExpr);
	}
}
