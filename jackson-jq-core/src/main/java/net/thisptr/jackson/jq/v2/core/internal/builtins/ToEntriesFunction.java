package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
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
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "to_entries", nargs = 0)
public class ToEntriesFunction implements Function {
	private static final TypeVariable T = TypeVariable.of("T");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(T, AnyType.getInstance()), FunctionType.of(
					ObjectType.of(T),
					ArrayType.of(ObjectType.of("key", StringType.getInstance(), "value", T)))));

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

			List<JsonNode> result = new ArrayList<>();
			JsonNodeType inType = jsonProvider.getNodeType(in);

			if (inType == JsonNodeType.OBJECT) {
				Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
				while (iter.hasNext()) {
					Map.Entry<String, JsonNode> entry = iter.next();
					Map<String, JsonNode> fields = new LinkedHashMap<>();
					fields.put("key", jsonProvider.createString(entry.getKey()));
					fields.put("value", entry.getValue());
					result.add(jsonProvider.createObject(fields));
				}
			} else if (inType == JsonNodeType.ARRAY) {
				Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
				for (int i = 0; iter.hasNext(); ++i) {
					JsonNode value = iter.next();
					Map<String, JsonNode> fields = new LinkedHashMap<>();
					fields.put("key", jsonProvider.createNumber(i));
					fields.put("value", value);
					result.add(jsonProvider.createObject(fields));
				}
			} else {
				throw new JsonQueryTypeException("%s has no keys", ExceptionMessages.describe(jsonProvider, version, in));
			}

			output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
		};
	}
}
