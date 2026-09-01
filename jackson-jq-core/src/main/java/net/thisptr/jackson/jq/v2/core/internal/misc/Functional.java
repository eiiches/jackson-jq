package net.thisptr.jackson.jq.v2.core.internal.misc;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class Functional {
	public interface Consumer<T> {
		void accept(T value) throws JsonQueryException;
	}
}
