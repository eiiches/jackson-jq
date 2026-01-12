package net.thisptr.jackson.jq.jackson3;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Jackson 3.
 * Runs the standard jq test suite using Jackson3JsonProviderImpl.
 */
public class Jackson3JsonQueryTest extends AbstractJsonQueryTest<JsonNode> {

	@Override
	protected Scope<JsonNode> createRootScope(Version version) {
		final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson3JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(version, scope);
		return scope;
	}

	@Override
	protected JsonNode parseTestNode(com.fasterxml.jackson.databind.JsonNode node) {
		// Convert Jackson 2 JsonNode (from test data) to Jackson 3 JsonNode via JSON string
		try {
			return Jackson3JsonProviderImpl.getInstance().fromString(node.toString());
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse test node", e);
		}
	}

	@Override
	protected Comparator<JsonNode> createComparator(boolean strictFieldOrder, double numericalErrors) {
		return new Jackson3JsonNodeComparator(strictFieldOrder, numericalErrors);
	}

	/**
	 * Provide test cases for JUnit parameterized tests.
	 */
	protected static Stream<String> defaultTestCases() throws java.io.IOException {
		return AbstractJsonQueryTest.defaultTestCases();
	}

	/**
	 * Custom comparator for Jackson3 JsonNode with support for numerical errors
	 * and optional strict field ordering.
	 */
	private static class Jackson3JsonNodeComparator extends JsonNodeComparator<JsonNode> {
		private static final long serialVersionUID = 1L;

		private final boolean strictFieldOrder;
		private final double numericalErrors;

		public Jackson3JsonNodeComparator(boolean strictFieldOrder, double numericalErrors) {
			super(Jackson3JsonProviderImpl.getInstance());
			this.strictFieldOrder = strictFieldOrder;
			this.numericalErrors = numericalErrors;
		}

		@Override
		protected int compareNumberNode(final JsonNode o1, final JsonNode o2) {
			if (Math.abs(o1.asDouble() - o2.asDouble()) < numericalErrors)
				return 0;
			return super.compareNumberNode(o1, o2);
		}

		@Override
		protected int compareObjectNode(final JsonNode o1, final JsonNode o2) {
			if (strictFieldOrder) {
				final Iterator<Entry<String, JsonNode>> it1 = o1.properties().iterator();
				final Iterator<Entry<String, JsonNode>> it2 = o2.properties().iterator();
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
}
