package net.thisptr.jackson.jq.jackson2;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Jackson 2.
 * Runs the standard jq test suite using Jackson2JsonProviderImpl.
 */
public class Jackson2JsonQueryTest extends AbstractJsonQueryTest<JsonNode> {

	@Override
	protected Scope<JsonNode> createRootScope(Version version) {
		final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(version, scope);
		return scope;
	}

	@Override
	protected JsonNode parseTestNode(JsonNode node) {
		// Test data is already in Jackson JsonNode format
		return node;
	}

	@Override
	protected Comparator<JsonNode> createComparator(boolean strictFieldOrder, double numericalErrors) {
		return new Jackson2JsonNodeComparator(strictFieldOrder, numericalErrors);
	}

	/**
	 * Provide test cases for JUnit parameterized tests.
	 */
	protected static Stream<String> defaultTestCases() throws java.io.IOException {
		return AbstractJsonQueryTest.defaultTestCases();
	}

	/**
	 * Custom comparator for Jackson2 JsonNode with support for numerical errors
	 * and optional strict field ordering.
	 */
	private static class Jackson2JsonNodeComparator extends JsonNodeComparator<JsonNode> {
		private static final long serialVersionUID = 1L;

		private final boolean strictFieldOrder;
		private final double numericalErrors;

		public Jackson2JsonNodeComparator(boolean strictFieldOrder, double numericalErrors) {
			super(Jackson2JsonProviderImpl.getInstance());
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
}
