package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;

import com.fasterxml.jackson.databind.JsonNode;

public class Conditional extends JsonQuery {
	private JsonQuery otherwise;
	private List<Pair<JsonQuery, JsonQuery>> switches;

	public Conditional(final List<Pair<JsonQuery, JsonQuery>> switches, final JsonQuery otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	private void applyRecursive(final List<JsonNode> out, final Scope scope, final List<Pair<JsonQuery, JsonQuery>> switches, final JsonNode in) throws JsonQueryException {
		final Pair<JsonQuery, JsonQuery> sw = switches.get(0);
		final List<JsonNode> rs = sw._1.apply(scope, in);
		for (final JsonNode r : rs) {
			if (JsonNodeUtils.asBoolean(r)) {
				out.addAll(sw._2.apply(scope, in));
			} else {
				if (switches.size() > 1) {
					applyRecursive(out, scope, switches.subList(1, switches.size()), in);
				} else {
					out.addAll(otherwise.apply(scope, in));
				}
			}
		}
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(out, scope, switches, in);
		return out;
	}

	@Override
	public String toString() {
		String ifstr = "if";
		final StringBuilder builder = new StringBuilder();
		for (final Pair<JsonQuery, JsonQuery> sw : switches) {
			builder.append(ifstr);
			builder.append(" ");
			builder.append(sw._1 != null ? sw._1 : "null");
			builder.append(" ");
			builder.append("then");
			builder.append(" ");
			builder.append(sw._2 != null ? sw._2 : "null");
			builder.append(" ");
			ifstr = "elif";
		}
		builder.append("else ");
		builder.append(otherwise != null ? otherwise : "null");
		builder.append(" ");
		builder.append("end");
		return builder.toString();
	}
}
