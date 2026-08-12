package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("nan/0")
public class NanFunction implements Function {
	@Override
	public <T> void apply(final Scope<T> scope, final List<Expression<T>> args, final T in, final Path<T> ipath, final PathOutput<T> output, final Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createDouble(Double.NaN), null);
	}
}
