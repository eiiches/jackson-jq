package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "from_entries", nargs = 0)
public class FromEntriesFunction implements Function {
	private static final TypeVariable T = TypeVariable.of("T");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(T, AnyType.getInstance()), FunctionType.of(
					Type.valueOf("[*:{key:STRING|UNDEFINED,Key:STRING|UNDEFINED,name:STRING|UNDEFINED,Name:STRING|UNDEFINED,value:T|UNDEFINED,Value:T|UNDEFINED,*:ANY}]"),
					ObjectType.of(T))));

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
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException("Cannot iterate over %s", ExceptionMessages.describe(jsonProvider, version, in));

			Map<String, JsonNode> result = new LinkedHashMap<>();
			Iterator<JsonNode> iter = inType == JsonNodeType.ARRAY
					? jsonProvider.getArrayElements(in)
					: jsonProvider.getObjectMemberValues(in);
			while (iter.hasNext()) {
				JsonNode entry = iter.next();
				if (!jsonProvider.isObject(entry))
					throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, entry, jsonProvider.createString("key")));

				@Var Maybe<JsonNode> key = jsonProvider.getObjectMember(entry, "key");
				if (key.isAbsent())
					key = jsonProvider.getObjectMember(entry, "Key");
				if (key.isAbsent())
					key = jsonProvider.getObjectMember(entry, "name");
				if (key.isAbsent())
					key = jsonProvider.getObjectMember(entry, "Name");
				if (key.isAbsent() || !jsonProvider.isString(key.get()))
					throw new JsonQueryTypeException("Cannot use %s as object key", ExceptionMessages.describe(jsonProvider, version, key.orElse(jsonProvider.createNull())));

				@Var Maybe<JsonNode> value = jsonProvider.getObjectMember(entry, "value");
				if (value.isAbsent())
					value = jsonProvider.getObjectMember(entry, "Value");

				result.put(jsonProvider.getString(key.get()), value.orElse(jsonProvider.createNull()));
			}

			RuntimeLimitChecks.checkObjectSize(scope.getRuntimeLimits(), result.size());
			output.emit(jsonProvider.createObject(result), UntrackedPath.getInstance());
		};
	}
}
