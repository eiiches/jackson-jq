package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "indices", nargs = 1)
public class IndicesFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), ArrayType.of(NumericType.of(NumberKind.INT)), FilterType.of(StringType.getInstance(), StringType.getInstance()))),
			TypeScheme.of(FunctionType.of(ArrayType.of(AnyType.getInstance()), ArrayType.of(NumericType.of(NumberKind.INT)), FilterType.of(ArrayType.of(AnyType.getInstance()), AnyType.getInstance()))),
			TypeScheme.of(FunctionType.of(NullType.getInstance(), NullType.getInstance(), FilterType.of(NullType.getInstance(), AnyType.getInstance()))));

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
		Version version = bindCtx.getJqVersion();
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> emitIndices(jsonProvider, needle, in, version, output));
		};
	}

	static <JsonNode> void emitIndices(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack, Version version, Output<JsonNode> output) throws JsonQueryException {
		JsonNodeType needleType = jsonProvider.getNodeType(needle);
		JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (haystackType == JsonNodeType.ARRAY || (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING)) {
			List<JsonNode> result = new ArrayList<>();
			for (int index : findIndices(jsonProvider, needle, haystack))
				result.add(jsonProvider.createNumber(index));
			output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
		} else if (needleType == JsonNodeType.STRING) {
			PathOperations.resolveObjectField(jsonProvider, haystack, UntrackedPath.getInstance(), output, jsonProvider.getString(needle), false, version);
		} else if (needleType == JsonNodeType.NUMBER) {
			PathOperations.resolveArrayIndex(jsonProvider, haystack, UntrackedPath.getInstance(), output, needle, false, version);
		} else if (needleType == JsonNodeType.ARRAY) {
			PathOperations.resolveArrayIndexOf(jsonProvider, haystack, UntrackedPath.getInstance(), output, needle, false, version);
		} else {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, haystack, needle));
		}
	}

	private static <JsonNode> List<Integer> findIndices(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		List<Integer> result = new ArrayList<>();
		JsonNodeType needleType = jsonProvider.getNodeType(needle);
		JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING) {
			String haystackText = jsonProvider.getString(haystack);
			String needleText = jsonProvider.getString(needle);
			if (!needleText.isEmpty()) {
				for (int index = haystackText.indexOf(needleText); index >= 0; index = haystackText.indexOf(needleText, index + 1))
					result.add(index);
			}
		} else if (needleType == JsonNodeType.ARRAY && haystackType == JsonNodeType.ARRAY) {
			int needleSize = jsonProvider.getArrayLength(needle);
			int haystackSize = jsonProvider.getArrayLength(haystack);
			if (needleSize != 0) {
				shift:
				for (int i = 0; i < haystackSize - needleSize + 1; ++i) {
					for (int j = 0; j < needleSize; ++j)
						if (comparator.compare(jsonProvider.getArrayElement(haystack, i + j), jsonProvider.getArrayElement(needle, j)) != 0)
							continue shift;
					result.add(i);
				}
			}
		} else if (haystackType == JsonNodeType.ARRAY) {
			int haystackSize = jsonProvider.getArrayLength(haystack);
			for (int i = 0; i < haystackSize; ++i)
				if (comparator.compare(jsonProvider.getArrayElement(haystack, i), needle) == 0)
					result.add(i);
		}
		return result;
	}
}
