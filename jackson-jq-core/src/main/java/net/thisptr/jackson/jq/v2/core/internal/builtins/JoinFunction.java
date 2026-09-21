package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "join", nargs = 1)
public class JoinFunction implements Function {
	/**
	 * Before jq 1.6 an element that is neither a string nor null cannot be joined.
	 */
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES_BEFORE_1_6;
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES;

	static {
		TYPE_SCHEMES_BEFORE_1_6 = typeSchemes(UnionType.of(StringType.getInstance(), NullType.getInstance()));
		TYPE_SCHEMES = typeSchemes(
				UnionType.of(StringType.getInstance(), NullType.getInstance(), NumericType.getInstance(), BooleanType.getInstance()));
	}

	private static List<TypeScheme<FunctionType>> typeSchemes(Type element) {
		Type separator = UnionType.of(StringType.getInstance(), NullType.getInstance());
		return List.of(
				TypeScheme.of(FunctionType.of(ArrayType.of(element), StringType.getInstance(), FilterType.of(ArrayType.of(element), separator))),
				TypeScheme.of(FunctionType.of(ObjectType.of(Map.of(), element), StringType.getInstance(), FilterType.of(ObjectType.of(Map.of(), element), separator))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return jqVersion.compareTo(Versions.JQ_1_6) >= 0 ? TYPE_SCHEMES : TYPE_SCHEMES_BEFORE_1_6;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(arguments.get(0).cardinality(), true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (sep, opath) -> {
				JsonNodeType inType = jsonProvider.getNodeType(in);
				if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
					throw new JsonQueryTypeException("Cannot iterate over %s", ExceptionMessages.describe(jsonProvider, version, in));

				RuntimeLimits limits = frame.getRuntimeLimits();
				@Var boolean first = true;
				StringBuilder builder = new StringBuilder();
				Iterator<JsonNode> iter = inType == JsonNodeType.ARRAY
						? jsonProvider.getArrayElements(in)
						: jsonProvider.getObjectMemberValues(in);
				while (iter.hasNext()) {
					JsonNode item = iter.next();
					if (!first) {
						JsonNodeType isepType = jsonProvider.getNodeType(sep);
						if (isepType == JsonNodeType.STRING) {
							append(limits, builder, jsonProvider.getString(sep));
						} else if (isepType == JsonNodeType.NULL) {
							// append nothing
						} else {
							throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, jsonProvider.createString(builder.toString())), ExceptionMessages.describe(jsonProvider, version, sep));
						}
					}

					JsonNodeType itemType = jsonProvider.getNodeType(item);
					if (itemType == JsonNodeType.STRING) {
						append(limits, builder, jsonProvider.getString(item));
					} else if (itemType == JsonNodeType.NULL) {
						// append nothing
					} else if (version.compareTo(Versions.JQ_1_6) >= 0 && (itemType == JsonNodeType.NUMBER || itemType == JsonNodeType.BOOLEAN)) {
						append(limits, builder, jsonProvider.format(item));
					} else {
						if (version.compareTo(Versions.JQ_1_6) >= 0)
							throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, jsonProvider.createString(builder.toString())), ExceptionMessages.describe(jsonProvider, version, item));
						throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, sep), ExceptionMessages.describe(jsonProvider, version, item));
					}

					first = false;
				}
				output.emit(jsonProvider.createString(builder.toString()), UntrackedPath.getInstance());
			});
		};
	}

	private static void append(RuntimeLimits limits, StringBuilder builder, String piece) {
		RuntimeLimitChecks.checkStringLength(limits, (long) builder.length() + piece.length());
		builder.append(piece);
	}
}
