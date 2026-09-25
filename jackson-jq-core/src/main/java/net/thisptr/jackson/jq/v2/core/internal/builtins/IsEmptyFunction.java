package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "isempty", nargs = 1, version = @VersionRangeSpec(
		min = @VersionSpec(major = 1, minor = 6, patch = 0)
))
public class IsEmptyFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, BooleanType.getInstance(), FilterType.of(INPUT, AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}


	/**
	 * Stops the generator once it has produced a value. jq expresses the same short-circuit with
	 * {@code label}/{@code break}, but those match by name, so nested {@code isempty} calls would
	 * intercept each other's break. The identity token makes each invocation catch only its own.
	 */
	private static final class ShortCircuit extends JsonQueryException {
		@Serial
		private static final long serialVersionUID = 1L;

		private final transient Object token;

		ShortCircuit(Object token) {
			super("break");
			this.token = token;
		}

		// A `try` inside the generator can intercept this, exactly as it can intercept jq's own
		// `break`, so surface the value jq surfaces there.
		@Override
		public <JsonNode> JsonNode toJson(JsonProvider<JsonNode> jsonProvider) {
			return jsonProvider.createObject(Collections.singletonMap("__jq", jsonProvider.createNumber(0)));
		}
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		Cardinality cardinality = jqVersion.compareTo(Versions.JQ_1_7) >= 0 ? Cardinality.ONE : Cardinality.UNKNOWN;
		return ExpressionPropertiesUtils.forwardDependencies(cardinality, false, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		// Up to jq 1.6 a `try` inside the generator swallows the short-circuit and re-enters its
		// catch body, which emits a second value -- reproducing jq 1.6's `//`-based definition.
		// From 1.7 on TryCatch tunnels downstream errors, so exactly one value is emitted.
		Cardinality cardinality = version.compareTo(Versions.JQ_1_7) >= 0 ? Cardinality.ONE : Cardinality.UNKNOWN;
		return (frame, in, ipath, output) -> {
			Object token = new Object();
			AtomicBoolean emitted = new AtomicBoolean();
			try {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (value, vpath) -> {
					emitted.set(true);
					output.emit(jsonProvider.createBoolean(false), UntrackedPath.getInstance());
					throw new ShortCircuit(token);
				});
			} catch (ShortCircuit e) {
				if (e.token != token)
					throw e; // belongs to an enclosing isempty; let it through
			}
			if (!emitted.get())
				output.emit(jsonProvider.createBoolean(true), UntrackedPath.getInstance());
		};
	}
}
