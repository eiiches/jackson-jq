package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("nan/0")
public class NanFunction<T> implements Function<T> {
	@Override
	public void apply(final Scope<T> scope, final List<Expression<T>> args, final T in, final Path<T> ipath, final PathOutput<T> output, final Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createDouble(Double.NaN), null);
	}
}