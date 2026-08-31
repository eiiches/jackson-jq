package net.thisptr.jackson.jq.v2.test;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * The comparator used to check test output against the golden data in {@code src/test/resources/tests}.
 *
 * <p>It adds two things to {@link JsonNodeComparator}: an optional numerical tolerance (the
 * {@code numerical_errors} test case property), and optional strict field ordering, which the jq
 * ordering deliberately ignores but the golden data is written to preserve.
 *
 * <p>Written against {@link JsonProvider} rather than any one JSON library so that every
 * {@code AbstractJsonQueryTest} subclass and {@link VerifyTestCasesTest} share one implementation.
 *
 * @param <T> the native JSON node type of {@code jsonProvider}
 */
public class TestJsonNodeComparator<T> extends JsonNodeComparator<T> {
	private static final long serialVersionUID = 1L;

	private final boolean strictFieldOrder;
	private final double numericalErrors;

	public TestJsonNodeComparator(JsonProvider<T> jsonProvider, boolean strictFieldOrder, double numericalErrors) {
		super(jsonProvider);
		this.strictFieldOrder = strictFieldOrder;
		this.numericalErrors = numericalErrors;
	}

	@Override
	protected int compareNumberNode(T o1, T o2) {
		if (numericalErrors > 0) {
			BigDecimal a = jsonProvider.getNumberAsBigDecimalExact(o1);
			BigDecimal b = jsonProvider.getNumberAsBigDecimalExact(o2);
			if (a != null && b != null && a.subtract(b).abs().compareTo(BigDecimal.valueOf(numericalErrors)) < 0)
				return 0;
		}
		return super.compareNumberNode(o1, o2);
	}

	@Override
	protected int compareObjectNode(T o1, T o2) {
		if (!strictFieldOrder)
			return super.compareObjectNode(o1, o2);

		Iterator<Map.Entry<String, T>> it1 = jsonProvider.getObjectEntries(o1);
		Iterator<Map.Entry<String, T>> it2 = jsonProvider.getObjectEntries(o2);
		while (it1.hasNext() && it2.hasNext()) {
			Map.Entry<String, T> entry1 = it1.next();
			Map.Entry<String, T> entry2 = it2.next();

			int r0 = entry1.getKey().compareTo(entry2.getKey());
			if (r0 != 0)
				return r0;

			int r1 = compare(entry1.getValue(), entry2.getValue());
			if (r1 != 0)
				return r1;
		}
		return Integer.compare(jsonProvider.size(o1), jsonProvider.size(o2));
	}
}
