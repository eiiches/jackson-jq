package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

public class Pair<T, U> {
	public final T _1;
	public final U _2;

	public Pair(T _1, U _2) {
		this._1 = _1;
		this._2 = _2;
	}

	public static <T, U> Pair<T, U> of(T _1, U _2) {
		return new Pair<>(_1, _2);
	}

	public static <T, U> List<T> _1(List<Pair<T, U>> items) {
		List<T> result = new ArrayList<>(items.size());
		for (Pair<T, ?> item : items)
			result.add(item._1);
		return result;
	}

	public static <T, U> List<U> _2(List<Pair<T, U>> items) {
		List<U> result = new ArrayList<>(items.size());
		for (Pair<?, U> item : items)
			result.add(item._2);
		return result;
	}

	@Override
	public String toString() {
		return String.format("(%s, %s)", _1, _2);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof Pair))
			return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(_1, pair._1) && Objects.equals(_2, pair._2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(_1, _2);
	}
}
