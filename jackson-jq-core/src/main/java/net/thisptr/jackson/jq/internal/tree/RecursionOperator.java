package net.thisptr.jackson.jq.internal.tree;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public class RecursionOperator implements Expression {
	private static void pathRecursive(final Scope scope, final JsonNode in, final Path path, final PathOutput output) throws JsonQueryException {
		output.emit(in, path);
		if (in.isObject()) {
			final Iterator<Entry<String, JsonNode>> iter = in.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				pathRecursive(scope, entry.getValue(), ObjectFieldPath.chainIfNotNull(path, entry.getKey()), output);
			}
		} else if (in.isArray()) {
			for (int i = 0; i < in.size(); ++i)
				pathRecursive(scope, in.get(i), ArrayIndexPath.chainIfNotNull(path, i), output);
		}
	}

	@Override
	public void apply(Scope scope, JsonNode in, Path path, PathOutput output, final boolean requirePath) throws JsonQueryException {
		pathRecursive(scope, in, path, output);
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
