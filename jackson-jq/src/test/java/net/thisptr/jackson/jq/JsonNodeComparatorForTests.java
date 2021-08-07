package net.thisptr.jackson.jq;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class JsonNodeComparatorForTests extends JsonNodeComparator {
	private static final long serialVersionUID = 1L;

	private final boolean strictFieldOrder;
	private final double numericalErrors;

	public JsonNodeComparatorForTests(final boolean strictFieldOrder, final double numericalErrors) {
		this.strictFieldOrder = strictFieldOrder;
		this.numericalErrors = numericalErrors;
	}

	@Override
	protected int compareNumberNode(final JsonNode o1, final JsonNode o2) {
		if (Math.abs(o1.doubleValue() - o2.doubleValue()) < numericalErrors)
			return 0;
		return super.compareNumberNode(o1, o2);
	}

	@Override
	protected int compareObjectNode(final JsonNode o1, final JsonNode o2) {
		if (strictFieldOrder) {
			final Iterator<Entry<String, JsonNode>> it1 = o1.fields();
			final Iterator<Entry<String, JsonNode>> it2 = o2.fields();
			while (it1.hasNext() && it2.hasNext()) {
				final Entry<String, JsonNode> entry1 = it1.next();
				final Entry<String, JsonNode> entry2 = it2.next();

				final int r0 = entry1.getKey().compareTo(entry2.getKey());
				if (r0 != 0)
					return r0;

				final int r1 = compare(entry1.getValue(), entry2.getValue());
				if (r1 != 0)
					return r1;
			}
			return Integer.compare(o1.size(), o2.size());
		} else {
			return super.compareObjectNode(o1, o2);
		}
	}
}
