package net.thisptr.jackson.jq.v2.core.internal.commons.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Lists {

	public static <T> List<T> newArrayList(Iterator<T> iter) {
		List<T> result = new ArrayList<>();
		while (iter.hasNext())
			result.add(iter.next());
		return result;
	}
}
