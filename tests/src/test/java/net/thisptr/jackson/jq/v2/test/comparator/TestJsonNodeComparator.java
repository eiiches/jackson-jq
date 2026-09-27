package net.thisptr.jackson.jq.v2.test.comparator;

import java.io.Serial;
import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * The comparator used to check test output against the golden data in {@code tests/test-cases}.
 *
 * <p>It adds two things to {@link JsonNodeComparator}: an optional floating-point tolerance (the
 * {@code float_tolerance} test case property), and optional strict field ordering, which the jq
 * ordering deliberately ignores but the golden data is written to preserve. Every harness turns
 * strict field ordering on, since jackson-jq reproduces jq's field order.
 *
 * <p>Written against {@link JsonProvider} rather than any one JSON library so that every
 * {@code AbstractJsonQueryTest} subclass shares one implementation.
 *
 * @param <T> the native JSON node type of {@code jsonProvider}
 */
public class TestJsonNodeComparator<T> extends JsonNodeComparator<T> {
	@Serial
	private static final long serialVersionUID = 1L;

	private final boolean strictFieldOrder;
	private final @Nullable FloatTolerance floatTolerance;

	public TestJsonNodeComparator(JsonProvider<T> jsonProvider, boolean strictFieldOrder, @Nullable FloatTolerance floatTolerance) {
		super(jsonProvider);
		this.strictFieldOrder = strictFieldOrder;
		this.floatTolerance = floatTolerance;
	}

	public TestJsonNodeComparator(JsonProvider<T> jsonProvider, boolean strictFieldOrder) {
		this(jsonProvider, strictFieldOrder, null);
	}

	private static long toUlpIndex(double v) {
		long bits = Double.doubleToRawLongBits(v);
		long mag = bits & 0x7FFF_FFFF_FFFF_FFFFL;
		return bits < 0 ? -mag : mag;
	}

	private static boolean withinUlps(double a, double b, long maxUlps) {
		if (Double.isNaN(a) || Double.isNaN(b))
			return false;
		if (Double.isInfinite(a) || Double.isInfinite(b))
			return Double.compare(a, b) == 0;
		long idxA = toUlpIndex(a);
		long idxB = toUlpIndex(b);
		if ((idxA < 0 && idxB > 0) || (idxA > 0 && idxB < 0)) {
			return (Math.abs(idxA) <= maxUlps) && (Math.abs(idxB) <= maxUlps - Math.abs(idxA));
		}
		return Math.abs(idxA - idxB) <= maxUlps;
	}

	@Override
	protected int compareNumberNode(T o1, T o2) {
		if (floatTolerance != null && floatTolerance.ulps() != null) {
			double a = jsonProvider.getNumberAsDoubleRounded(o1);
			double b = jsonProvider.getNumberAsDoubleRounded(o2);
			if (withinUlps(a, b, floatTolerance.ulps()))
				return 0;
		}
		return super.compareNumberNode(o1, o2);
	}

	@Override
	protected int compareObjectNode(T o1, T o2) {
		if (!strictFieldOrder)
			return super.compareObjectNode(o1, o2);

		Iterator<Map.Entry<String, T>> it1 = jsonProvider.getObjectMembers(o1);
		Iterator<Map.Entry<String, T>> it2 = jsonProvider.getObjectMembers(o2);
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
		return Integer.compare(jsonProvider.getObjectMemberCount(o1), jsonProvider.getObjectMemberCount(o2));
	}
}
