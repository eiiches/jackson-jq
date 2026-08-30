package net.thisptr.jackson.jq.v2.jakarta;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Runs the standard jq test suite with Jakarta JSON Processing.
 */
public class JakartaJsonQueryTest extends AbstractJsonQueryTest<JsonValue> {

	@Override
	protected EnvironmentBuilder<JsonValue> createEnvironment(Version version) {
		return new EnvironmentBuilder<>(JakartaJsonProviderImpl.getInstance(), version);
	}

	@Override
	protected JsonValue parseTestNode(JsonNode node) {
		return JakartaJsonProviderImpl.getInstance().parse(node.toString());
	}

	@Override
	protected Comparator<JsonValue> createComparator(boolean strictFieldOrder, double numericalErrors) {
		return new JakartaJsonNodeComparator(strictFieldOrder, numericalErrors);
	}

	private static class JakartaJsonNodeComparator extends JsonNodeComparator<JsonValue> {
		private static final long serialVersionUID = 1L;

		private final boolean strictFieldOrder;
		private final double numericalErrors;

		JakartaJsonNodeComparator(boolean strictFieldOrder, double numericalErrors) {
			super(JakartaJsonProviderImpl.getInstance());
			this.strictFieldOrder = strictFieldOrder;
			this.numericalErrors = numericalErrors;
		}

		@Override
		protected int compareNumberNode(JsonValue o1, JsonValue o2) {
			if (Math.abs(((JsonNumber) o1).doubleValue() - ((JsonNumber) o2).doubleValue()) < numericalErrors)
				return 0;
			return super.compareNumberNode(o1, o2);
		}

		@Override
		protected int compareObjectNode(JsonValue o1, JsonValue o2) {
			if (!strictFieldOrder)
				return super.compareObjectNode(o1, o2);
			Iterator<Map.Entry<String, JsonValue>> it1 = ((JsonObject) o1).entrySet().iterator();
			Iterator<Map.Entry<String, JsonValue>> it2 = ((JsonObject) o2).entrySet().iterator();
			while (it1.hasNext() && it2.hasNext()) {
				Map.Entry<String, JsonValue> entry1 = it1.next();
				Map.Entry<String, JsonValue> entry2 = it2.next();
				int keyResult = entry1.getKey().compareTo(entry2.getKey());
				if (keyResult != 0)
					return keyResult;
				int valueResult = compare(entry1.getValue(), entry2.getValue());
				if (valueResult != 0)
					return valueResult;
			}
			return Integer.compare(((JsonObject) o1).size(), ((JsonObject) o2).size());
		}
	}
}
