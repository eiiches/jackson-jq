package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

@FunctionalInterface
public interface FunctionFactory {
	Function createFunction(List<Expression> args, Version version);
}
