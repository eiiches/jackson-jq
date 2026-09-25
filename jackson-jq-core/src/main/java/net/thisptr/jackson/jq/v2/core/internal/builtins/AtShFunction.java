package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.Strings;
import net.thisptr.jackson.jq.v2.core.internal.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
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
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "@sh", nargs = 0)
public class AtShFunction implements Function {
	private static final Type SCALAR = UnionType.of(StringType.getInstance(), NumericType.getInstance(), BooleanType.getInstance(), NullType.getInstance());
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(UnionType.of(SCALAR, ArrayType.of(SCALAR)), StringType.getInstance())));

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
		return (scope, in, ipath, output) -> {
			JsonNodeType type = jsonProvider.getNodeType(in);
			if (type == JsonNodeType.ARRAY) {
				List<String> tokens = new ArrayList<>();
				Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
				while (iter.hasNext()) {
					JsonNode i = iter.next();
					JsonNodeType iType = jsonProvider.getNodeType(i);
					if (iType == JsonNodeType.STRING) {
						tokens.add(escape(jsonProvider.getString(i)));
					} else if (isValueNode(iType)) {
						tokens.add(JsonNodeUtils.toString(jsonProvider, i, version));
					} else {
						throw new IllegalJsonInputException(iType + " cannot be escaped for shell");
					}
				}
				output.emit(jsonProvider.createString(Strings.join(" ", tokens)), UntrackedPath.getInstance());
			} else if (type == JsonNodeType.STRING) {
				output.emit(jsonProvider.createString(escape(jsonProvider.getString(in))), UntrackedPath.getInstance());
			} else if (isValueNode(type)) {
				output.emit(jsonProvider.createString(JsonNodeUtils.toString(jsonProvider, in, version)), UntrackedPath.getInstance());
			} else {
				throw new IllegalJsonInputException(type + " cannot be escaped for shell");
			}
		};
	}

	private static boolean isValueNode(JsonNodeType type) {
		return type == JsonNodeType.STRING || type == JsonNodeType.NUMBER || type == JsonNodeType.BOOLEAN || type == JsonNodeType.NULL;
	}

	public String escape(String text) {
		StringBuilder builder = new StringBuilder("'");
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
			switch (ch) {
				case '\'':
					builder.append("'\\''");
					break;
				case '\0':
					builder.append("\\0");
					break;
				default:
					builder.append(ch);
					break;
			}
		}
		builder.append("'");
		return builder.toString();
	}
}
