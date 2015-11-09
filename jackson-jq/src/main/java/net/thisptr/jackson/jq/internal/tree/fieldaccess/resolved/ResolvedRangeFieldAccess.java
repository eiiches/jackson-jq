package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Range;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ResolvedRangeFieldAccess extends ResolvedFieldAccess {
	private List<Range> ranges;

	public ResolvedRangeFieldAccess(final boolean permissive, final List<Range> ranges) {
		super(permissive);
		this.ranges = ranges;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final Range range : ranges) {
			if (in.isArray()) {
				final Range r = range.over(in.size());
				final ArrayNode array = scope.getObjectMapper().createArrayNode();
				for (int index = (int) r.begin; index < (int) r.end; ++index)
					array.add(in.get(index));
				out.add(array);
			} else if (in.isTextual()) {
				final String tmp = in.asText();
				final Range r = range.over(tmp.length());
				out.add(new TextNode(tmp.substring((int) r.begin, (int) r.end)));
			} else if (in.isNull()) {
				out.add(NullNode.getInstance());
			} else {
				if (!permissive)
					throw JsonQueryException.format("Cannot index %s with object", in.getNodeType());
			}
		}
		return out;
	}

	public List<Range> ranges() {
		return Collections.unmodifiableList(ranges);
	}
}