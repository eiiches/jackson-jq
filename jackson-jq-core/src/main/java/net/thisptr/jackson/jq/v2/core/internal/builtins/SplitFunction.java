package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.Strings;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
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
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "split", nargs = 1)
public class SplitFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), ArrayType.of(StringType.getInstance()), FilterType.of(StringType.getInstance(), StringType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (sep, opath) -> {
				if (!jsonProvider.isString(in) || !jsonProvider.isString(sep))
					throw new JsonQueryTypeException("split input and separator must be strings");

				List<JsonNode> row = new ArrayList<>();
				for (String seg : Strings.split(jsonProvider.getString(in), jsonProvider.getString(sep)))
					row.add(jsonProvider.createString(seg));

				output.emit(jsonProvider.createArray(row), UntrackedPath.getInstance());
			});
		};
	}
}
