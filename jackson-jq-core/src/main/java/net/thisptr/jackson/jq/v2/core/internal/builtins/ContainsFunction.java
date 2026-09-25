package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
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
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "contains", nargs = 1)
public class ContainsFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, BooleanType.getInstance(), FilterType.of(INPUT, INPUT))));

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
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (value, opath) -> {
				if (jsonProvider.getNodeType(in) != jsonProvider.getNodeType(value)
						|| (jsonProvider.isBoolean(in) && jsonProvider.getBoolean(in) != jsonProvider.getBoolean(value))) {
					throw new JsonQueryTypeException("%s and %s cannot have their containment checked", ExceptionMessages.describe(jsonProvider, version, in), ExceptionMessages.describe(jsonProvider, version, value));
				}
				output.emit(jsonProvider.createBoolean(contains(jsonProvider, value, in)), UntrackedPath.getInstance());
			});
		};
	}

	private static <JsonNode> boolean contains(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) {
		JsonNodeType hType = jsonProvider.getNodeType(haystack);
		JsonNodeType nType = jsonProvider.getNodeType(needle);
		if (hType == JsonNodeType.STRING && nType == JsonNodeType.STRING) {
			return jsonProvider.getString(haystack).contains(jsonProvider.getString(needle));
		} else if (hType == JsonNodeType.ARRAY && nType == JsonNodeType.ARRAY) {
			Iterator<JsonNode> nIter = jsonProvider.getArrayElements(needle);
			while (nIter.hasNext()) {
				JsonNode n = nIter.next();
				@Var boolean found = false;
				Iterator<JsonNode> hIter = jsonProvider.getArrayElements(haystack);
				while (hIter.hasNext()) {
					JsonNode h = hIter.next();
					if (contains(jsonProvider, n, h)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		} else if (hType == JsonNodeType.OBJECT && nType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(needle);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> field = iter.next();
				Maybe<JsonNode> tmp = jsonProvider.getObjectMember(haystack, field.getKey());
				if (tmp.isAbsent())
					return false;
				if (!contains(jsonProvider, field.getValue(), tmp.get()))
					return false;
			}
			return true;
		} else {
			return new JsonNodeComparator<>(jsonProvider).compare(haystack, needle) == 0;
		}
	}
}
