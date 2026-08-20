package net.thisptr.jackson.jq.v2.gson;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Gson.
 * Runs the standard jq test suite using GsonJsonProviderImpl.
 */
public class GsonJsonQueryTest extends AbstractJsonQueryTest<JsonElement> {

	@Override
	protected EnvironmentBuilder<JsonElement> createEnvironment(Version version) {
		return new EnvironmentBuilder<>(GsonJsonProviderImpl.getInstance(), version);
	}

	@Override
	protected JsonElement parseTestNode(JsonNode node) {
		// Convert Jackson JsonNode to Gson JsonElement via JSON string
		return JsonParser.parseString(node.toString());
	}

	@Override
	protected Comparator<JsonElement> createComparator(boolean strictFieldOrder, double numericalErrors) {
		return new GsonJsonNodeComparator(strictFieldOrder, numericalErrors);
	}

	/**
	 * Provide test cases for JUnit parameterized tests.
	 */
	protected static Stream<String> defaultTestCases() throws IOException {
		return AbstractJsonQueryTest.defaultTestCases(GsonJsonQueryTest.class.getClassLoader());
	}

	/**
	 * Custom comparator for Gson JsonElement with support for numerical errors
	 * and optional strict field ordering.
	 */
	private static class GsonJsonNodeComparator extends JsonNodeComparator<JsonElement> {
		private static long serialVersionUID = 1L;

		private boolean strictFieldOrder;
		private double numericalErrors;

		GsonJsonNodeComparator(boolean strictFieldOrder, double numericalErrors) {
			super(GsonJsonProviderImpl.getInstance());
			this.strictFieldOrder = strictFieldOrder;
			this.numericalErrors = numericalErrors;
		}

		@Override
		protected int compareNumberNode(JsonElement o1, JsonElement o2) {
			if (Math.abs(o1.getAsDouble() - o2.getAsDouble()) < numericalErrors)
				return 0;
			return super.compareNumberNode(o1, o2);
		}

		@Override
		protected int compareObjectNode(JsonElement o1, JsonElement o2) {
			if (strictFieldOrder) {
				Iterator<Map.Entry<String, JsonElement>> it1 = o1.getAsJsonObject().entrySet().iterator();
				Iterator<Map.Entry<String, JsonElement>> it2 = o2.getAsJsonObject().entrySet().iterator();
				while (it1.hasNext() && it2.hasNext()) {
					Map.Entry<String, JsonElement> entry1 = it1.next();
					Map.Entry<String, JsonElement> entry2 = it2.next();

					int r0 = entry1.getKey().compareTo(entry2.getKey());
					if (r0 != 0)
						return r0;

					int r1 = compare(entry1.getValue(), entry2.getValue());
					if (r1 != 0)
						return r1;
				}
				return Integer.compare(o1.getAsJsonObject().size(), o2.getAsJsonObject().size());
			} else {
				return super.compareObjectNode(o1, o2);
			}
		}
	}
}
