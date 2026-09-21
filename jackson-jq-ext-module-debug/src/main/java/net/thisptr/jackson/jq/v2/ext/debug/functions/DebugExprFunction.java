package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class DebugExprFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	/**
	 * The argument is inspected at bind time and never evaluated, so it constrains nothing.
	 */
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, ObjectType.of("depends_on_input", BooleanType.getInstance(), "depends_on_external_state", BooleanType.getInstance()), FilterType.of(INPUT, AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return new ExpressionProperties(Cardinality.ONE, false, false);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		ExpressionProperties properties = compilerProperties(args.get(0));
		return (context, in, ipath, output) -> {
			Map<String, JsonNode> info = new LinkedHashMap<>();
			info.put("depends_on_input", jsonProvider.createBoolean(properties.dependsOnInput()));
			info.put("depends_on_external_state", jsonProvider.createBoolean(properties.dependsOnExternalState()));
			output.emit(jsonProvider.createObject(info), UntrackedPath.getInstance());
		};
	}

	/**
	 * This diagnostic module is explicitly opened to core internals. Keep that dependency reflective so
	 * the public expression SPI remains execution-only.
	 */
	private static ExpressionProperties compilerProperties(Expression<?, ?> expression) {
		try {
			Cardinality cardinality = (Cardinality) expression.getClass().getMethod("getCardinality").invoke(expression);
			boolean dependsOnInput = (Boolean) expression.getClass().getMethod("dependsOnInput").invoke(expression);
			boolean dependsOnExternalState = (Boolean) expression.getClass().getMethod("dependsOnExternalState").invoke(expression);
			return new ExpressionProperties(cardinality, dependsOnInput, dependsOnExternalState);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return ExpressionProperties.UNKNOWN;
		}
	}
}
