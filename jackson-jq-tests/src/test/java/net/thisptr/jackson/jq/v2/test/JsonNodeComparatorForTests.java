package net.thisptr.jackson.jq.v2.test;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

public class JsonNodeComparatorForTests extends JsonNodeComparator<JsonNode> {
	private static final long serialVersionUID = 1L;

	private final boolean strictFieldOrder;
	private final double numericalErrors;

	public JsonNodeComparatorForTests(boolean strictFieldOrder, double numericalErrors) {
		super(Jackson2JsonProviderImpl.getInstance());
		this.strictFieldOrder = strictFieldOrder;
		this.numericalErrors = numericalErrors;
	}

	@Override
	protected int compareNumberNode(JsonNode o1, JsonNode o2) {
		if (Math.abs(o1.doubleValue() - o2.doubleValue()) < numericalErrors)
			return 0;
		return super.compareNumberNode(o1, o2);
	}

	@Override
	protected int compareObjectNode(JsonNode o1, JsonNode o2) {
		if (strictFieldOrder) {
			Iterator<Entry<String, JsonNode>> it1 = o1.fields();
			Iterator<Entry<String, JsonNode>> it2 = o2.fields();
			while (it1.hasNext() && it2.hasNext()) {
				Entry<String, JsonNode> entry1 = it1.next();
				Entry<String, JsonNode> entry2 = it2.next();

				int r0 = entry1.getKey().compareTo(entry2.getKey());
				if (r0 != 0)
					return r0;

				int r1 = compare(entry1.getValue(), entry2.getValue());
				if (r1 != 0)
					return r1;
			}
			return Integer.compare(o1.size(), o2.size());
		} else {
			return super.compareObjectNode(o1, o2);
		}
	}
}
