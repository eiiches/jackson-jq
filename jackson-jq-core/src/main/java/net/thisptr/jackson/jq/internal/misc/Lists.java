package net.thisptr.jackson.jq.internal.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Lists {

	public static <T> List<T> newArrayList(final Iterator<T> iter) {
		final List<T> result = new ArrayList<>();
		while (iter.hasNext())
			result.add(iter.next());
		return result;
	}

	public static <T> List<T> newArrayList(final Iterable<T> iter) {
		return newArrayList(iter.iterator());
	}

}
