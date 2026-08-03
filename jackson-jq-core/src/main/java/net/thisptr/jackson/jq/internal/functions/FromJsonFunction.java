package net.thisptr.jackson.jq.internal.functions;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("fromjson/0")
public class FromJsonFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (!in.isTextual())
			throw new JsonQueryTypeException("%s only strings can be parsed", in);

		final JsonNode tree;
		try (final JsonParser parser = scope.getObjectMapper().getFactory().createParser(in.asText())) {
			tree = parser.readValueAsTree();
			if (tree == null)
				throw new JsonQueryException("failed to parse %s as json; empty", in);
			if (parser.nextToken() != null)
				throw new JsonQueryException("failed to parse %s as json; trailing data", in);
		} catch (final JsonQueryException e) {
			throw e;
		} catch (final IOException e) {
			throw new JsonQueryException("failed to parse %s as json", in);
		}
		output.emit(tree, null);
	}
}
