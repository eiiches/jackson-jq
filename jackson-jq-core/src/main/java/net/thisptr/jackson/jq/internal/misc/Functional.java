package net.thisptr.jackson.jq.internal.misc;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public class Functional {
	public interface Consumer<T> {
		void accept(T value) throws JsonQueryException;
	}
}
