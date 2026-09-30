package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.UnicodeUtils;
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
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "reverse", nargs = 0)
public class ReverseFunction implements Function {
	private static final TypeVariable ELEMENT = TypeVariable.of("Element");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(ELEMENT, AnyType.getInstance()), FunctionType.of(ArrayType.of(ELEMENT), ArrayType.of(ELEMENT))),
			TypeScheme.of(FunctionType.of(NullType.getInstance(), ArrayType.of(NeverType.getInstance()))));

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
			JsonNode emptyArray = jsonProvider.createArray(Collections.emptyList());

			JsonNodeType type = jsonProvider.getNodeType(in);
			if (type == JsonNodeType.NULL) {
				output.emit(emptyArray, UntrackedPath.getInstance());
				return;
			}
			if (type == JsonNodeType.ARRAY) {
				int size = jsonProvider.getArrayLength(in);
				for (int i = size - 1; i >= 0; --i)
					result.add(jsonProvider.getArrayElement(in, i));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
				return;
			}

			// Below are to emulate jq behavior. jq defines reverse as [.[length - 1 - range(0;length)]],
			// so an input that has a length but cannot be indexed by a number fails on the very first
			// index it tries, length - 1, and a length of zero indexes nothing and answers [].

			if (type == JsonNodeType.STRING) {
				int length = UnicodeUtils.lengthUtf32(jsonProvider.getString(in));
				if (length == 0) {
					output.emit(emptyArray, UntrackedPath.getInstance());
					return;
				}
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(length - 1)));
			}
			if (type == JsonNodeType.NUMBER) {
				// The index keeps jq's double arithmetic, hence createNumber(double) rather than
				// JsonNodeUtils#asNumericNode: a whole value beyond a double's mantissa renders the way jq
				// renders it, e.g. 2871948651097801000 rather than the exact 2871948651097801216. NaN is not
				// equal to zero, so it takes the error path as it does in jq.
				double length = Math.abs(jsonProvider.getNumberAsDoubleRounded(in));
				if (length == 0.0) {
					output.emit(emptyArray, UntrackedPath.getInstance());
					return;
				}
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(length - 1)));
			}
			if (type == JsonNodeType.OBJECT) {
				int length = jsonProvider.getObjectMemberCount(in);
				if (length == 0) {
					output.emit(emptyArray, UntrackedPath.getInstance());
					return;
				}
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(length - 1)));
			}
			if (type == JsonNodeType.BOOLEAN) {
				throw new JsonQueryTypeException("%s has no length", ExceptionMessages.describe(jsonProvider, version, in));
			}
			throw new JsonQueryTypeException("%s cannot be reversed", ExceptionMessages.describe(jsonProvider, version, in));
		};
	}
}
