package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.function.Function;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.spi.Cardinality;

public class CardinalityUtils {

	public static Cardinality multiply(Cardinality a, Cardinality b) {
		if (a == Cardinality.ZERO || b == Cardinality.ZERO)
			return Cardinality.ZERO;
		if (a == Cardinality.ONE && b == Cardinality.ONE)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	public static Cardinality multiply(Cardinality a, Cardinality b, Cardinality c) {
		return multiply(multiply(a, b), c);
	}

	public static Cardinality multiply(Cardinality a, Cardinality b, Cardinality c, Cardinality d) {
		return multiply(multiply(multiply(a, b), c), d);
	}

	public static <T> Cardinality multiply(Iterable<T> items, Function<? super T, Cardinality> extractor) {
		@Var boolean allOne = true;
		for (T item : items) {
			Cardinality c = extractor.apply(item);
			if (c == Cardinality.ZERO)
				return Cardinality.ZERO;
			if (c != Cardinality.ONE)
				allOne = false;
		}
		return allOne ? Cardinality.ONE : Cardinality.UNKNOWN;
	}

	public static Cardinality sum(Cardinality a, Cardinality b) {
		if (a == Cardinality.UNKNOWN || b == Cardinality.UNKNOWN)
			return Cardinality.UNKNOWN;
		if (a == Cardinality.ZERO && b == Cardinality.ZERO)
			return Cardinality.ZERO;
		if ((a == Cardinality.ONE && b == Cardinality.ZERO) || (a == Cardinality.ZERO && b == Cardinality.ONE))
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	public static <T> Cardinality sum(Iterable<T> items, Function<? super T, Cardinality> extractor) {
		@Var int oneCount = 0;
		for (T item : items) {
			Cardinality c = extractor.apply(item);
			if (c == Cardinality.UNKNOWN)
				return Cardinality.UNKNOWN;
			if (c == Cardinality.ONE)
				oneCount++;
		}
		if (oneCount == 0)
			return Cardinality.ZERO;
		if (oneCount == 1)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	public static Cardinality alternative(Cardinality lhs, Cardinality rhs) {
		if (lhs == Cardinality.ZERO)
			return rhs;
		if (lhs == Cardinality.ONE && rhs == Cardinality.ONE)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}
}
