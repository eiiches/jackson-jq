package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.LegacyFunction;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "infinite", nargs = 0)
public class InfiniteFunction implements LegacyFunction {
	@Override
	public <T> void apply(Scope<T> scope, List<Expression> args, T in, @Nullable Path<T> ipath, PathOutput<T> output, Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createNumber(Double.POSITIVE_INFINITY), null);
	}
}
