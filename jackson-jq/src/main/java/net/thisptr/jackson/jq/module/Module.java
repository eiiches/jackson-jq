package net.thisptr.jackson.jq.module;

import java.util.Map;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.annotations.Experimental;

@Experimental
public interface Module {
	Function getFunction(String fname, int nargs);

	Map<String, Function> getAllFunctions();
}
