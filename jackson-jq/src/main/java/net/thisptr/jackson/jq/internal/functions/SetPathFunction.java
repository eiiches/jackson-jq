package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.PathUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("setpath/2")
public class SetPathFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path inpath, final PathOutput output, final Version version) throws JsonQueryException {
		args.get(1).apply(scope, in, (newvalnode) -> {
			args.get(0).apply(scope, in, (pathnode) -> {
				final Path path = PathUtils.toPath(pathnode);
				final JsonNode out = path.mutate(in, (dummy) -> newvalnode);
				output.emit(out, path);
			});
		});
	}
}
