package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryUserException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction({ "error/0", "error/1" })
public class ErrorFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (args.size() == 0) {
			if (in.isNull())
				return;
			throw new JsonQueryUserException(in);
		} else {
			args.get(0).apply(scope, in, (out) -> {
				if (out.isNull())
					return;
				throw new JsonQueryUserException(out);
			});
		}
	}
}
