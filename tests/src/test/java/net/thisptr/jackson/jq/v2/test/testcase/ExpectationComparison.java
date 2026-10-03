package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.test.comparator.FloatTolerance;
import net.thisptr.jackson.jq.v2.test.comparator.TestJsonNodeComparator;

/**
 * Compares results for deciding whether an override describes a meaningful incompatibility.
 */
public final class ExpectationComparison {
	public static boolean equivalent(TestCase.AbstractExpectation reference, TestCase.AbstractExpectation actual,
			@Nullable FloatTolerance tolerance) {
		if (limited(reference) || limited(actual))
			return limited(reference) && limited(actual);
		boolean referenceError = reference.compileError != null || reference.runtimeError != null;
		boolean actualError = actual.compileError != null || actual.runtimeError != null;
		if (referenceError || actualError)
			return referenceError && actualError;
		return sameValues(reference.output, actual.output, tolerance);
	}

	private static boolean limited(TestCase.AbstractExpectation row) {
		return row.timedOut() || row.limitExceeded();
	}

	/**
	 * Whether two recorded outputs are the same to every assertion, which compares values rather
	 * than how a number happens to be written.
	 *
	 * @param left one recorded output, or null for a row that records no output
	 * @param right the other recorded output, or null for a row that records no output
	 * @param tolerance the case's float tolerance, or null to compare exactly
	 * @return whether the outputs are indistinguishable
	 */
	public static boolean sameValues(@Nullable List<JsonNode> left, @Nullable List<JsonNode> right,
			@Nullable FloatTolerance tolerance) {
		if (left == null || right == null)
			return left == right;
		if (left.size() != right.size())
			return false;
		TestJsonNodeComparator<JsonNode> comparator = new TestJsonNodeComparator<>(
				Jackson2JsonProvider.getInstance(), true, tolerance);
		for (int i = 0; i < left.size(); i++) {
			if (comparator.compare(left.get(i), right.get(i)) != 0)
				return false;
		}
		return true;
	}

	private ExpectationComparison() {
	}
}
