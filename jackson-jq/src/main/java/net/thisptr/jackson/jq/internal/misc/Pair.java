package net.thisptr.jackson.jq.internal.misc;

import java.util.ArrayList;
import java.util.List;

public class Pair<T, U> {
	public final T _1;
	public final U _2;

	public Pair(final T _1, final U _2) {
		this._1 = _1;
		this._2 = _2;
	}

	public static <T, U> Pair<T, U> of(final T _1, final U _2) {
		return new Pair<>(_1, _2);
	}

	public static <T, U> List<T> _1(final List<Pair<T, U>> items) {
		final List<T> result = new ArrayList<>(items.size());
		for (final Pair<T, ?> item : items)
			result.add(item._1);
		return result;
	}

	public static <T, U> List<U> _2(final List<Pair<T, U>> items) {
		final List<U> result = new ArrayList<>(items.size());
		for (final Pair<?, U> item : items)
			result.add(item._2);
		return result;
	}
}