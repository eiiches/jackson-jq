package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ModuleMetaBindContext;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ModuleMetaLookup;
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
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Inspects a module without compiling it or loading its dependencies.
 */
@FunctionRegistration(name = "modulemeta", nargs = 0)
public final class ModuleMetaFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), ObjectType.of(AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, true, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		if (!(bindCtx instanceof ModuleMetaBindContext<?> scoped))
			throw new IllegalStateException("modulemeta requires a module scope");
		// The compiler creates this context and lookup together for the same JsonNode type.
		@SuppressWarnings("unchecked")
		ModuleMetaLookup<JsonNode> moduleLookup = (ModuleMetaLookup<JsonNode>) scoped.getModuleMetaLookup();
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version jqVersion = bindCtx.getJqVersion();
		return (scope, in, ipath, output) -> {
			if (!jsonProvider.isString(in))
				throw new JsonQueryException("modulemeta input module name must be a string");
			output.emit(moduleLookup.inspect(jsonProvider.getString(in), jsonProvider, jqVersion), UntrackedPath.getInstance());
		};
	}
}
