package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Range;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

public class ResolvedRangeFieldAccess extends ResolvedFieldAccess {
	private List<Range> ranges;

	public ResolvedRangeFieldAccess(final boolean permissive, final List<Range> ranges) {
		super(permissive);
		this.ranges = ranges;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		for (final Range range : ranges) {
			if (in.isArray()) {
				final Range r = range.over(in.size());
				final ArrayNode array = scope.getObjectMapper().createArrayNode();
				for (int index = (int) r.begin; index < (int) r.end; ++index)
					array.add(in.get(index));
				output.emit(array);
			} else if (in.isTextual()) {
				final String _in = in.asText();
				final Range r = range.over(UnicodeUtils.lengthUtf32(_in));
				output.emit(new TextNode(UnicodeUtils.substringUtf32(_in, (int) r.begin, (int) r.end)));
			} else if (in.isNull()) {
				output.emit(NullNode.getInstance());
			} else {
				if (!permissive)
					throw JsonQueryException.format("Cannot index %s with object", in.getNodeType());
			}
		}
	}

	public List<Range> ranges() {
		return Collections.unmodifiableList(ranges);
	}
}