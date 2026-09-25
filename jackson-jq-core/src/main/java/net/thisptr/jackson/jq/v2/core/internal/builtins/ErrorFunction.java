package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryUserException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "error", nargs = 0)
@FunctionRegistration(name = "error", nargs = 1)
public class ErrorFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	/**
	 * Indexed by argument count.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), NeverType.getInstance()))),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NeverType.getInstance(), FilterType.of(INPUT, AnyType.getInstance())))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 0 || totalArguments > 1)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ZERO, arguments.isEmpty(), false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, in, ipath, output) -> {
			if (args.isEmpty()) {
				if (jsonProvider.isNull(in))
					return;
				throw new JsonQueryUserException(jsonProvider, in);
			} else {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> {
					if (jsonProvider.isNull(out))
						return;
					throw new JsonQueryUserException(jsonProvider, out);
				});
			}
		};
	}
}
