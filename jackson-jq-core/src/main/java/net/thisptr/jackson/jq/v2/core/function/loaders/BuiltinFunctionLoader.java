package net.thisptr.jackson.jq.v2.core.function.loaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AtBase64Function;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AtBase64dFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AtHtmlFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AtShFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AtUriFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ContainsFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.DelPathsFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.EmptyFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.EndsWithFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ErrorFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ExplodeFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.FromDateIso8601Function;
import net.thisptr.jackson.jq.v2.core.internal.builtins.FromEntriesFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.FromJsonFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.GetPathFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.GroupByFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.HasFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ImplodeFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IndexFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IndicesFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.InfiniteFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IsEmptyFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IsInfiniteFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IsNanFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IsNormalFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.JoinFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.KeysFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.KeysUnsortedFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.LTrimStrFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.LengthFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.MaxByFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.MinByFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ModuleMetaFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.NanFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.NotFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.NowFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.PathFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.PathsFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.RIndexFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.RTrimStrFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.RangeFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ReverseFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.SetPathFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.SortByFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.SplitFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.StartsWithFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ToDateIso8601Function;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ToEntriesFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ToJsonFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ToNumberFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ToStringFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.TypeFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.Utf8ByteLengthFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.filters.CsvFilter;
import net.thisptr.jackson.jq.v2.core.internal.builtins.filters.TsvFilter;
import net.thisptr.jackson.jq.v2.core.internal.builtins.library.CoreJqLibrary;
import net.thisptr.jackson.jq.v2.core.internal.builtins.math.Atan2Function;
import net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions;
import net.thisptr.jackson.jq.v2.core.internal.builtins.math.PowFunction;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

/**
 * The jq builtins, registered in code rather than discovered.
 * <p>
 * This is the loader {@link net.thisptr.jackson.jq.v2.core.EnvironmentBuilder#withDefaultLoaders} installs,
 * and the only place the set of builtins is written down: every {@link Function} and every
 * {@link JqLibrary} the engine ships is constructed by this class, and each one still states its own
 * name, arity and applicable jq versions through {@link FunctionRegistration} (or, for a jq-language
 * definition, through the {@link JqFunction} itself). Nothing about the builtins depends on
 * {@link java.util.ServiceLoader}, {@code META-INF/services}, or the shape of the jar they are packaged in.
 * <p>
 * Registering a function the engine does not ship is what {@link ClassPathFunctionLoader} and
 * {@link net.thisptr.jackson.jq.v2.core.EnvironmentBuilder#addFunctionLoader} are for.
 */
public final class BuiltinFunctionLoader implements FunctionLoader {
	private static final List<Function> FUNCTIONS = List.of(
			new Atan2Function(),
			new AtBase64dFunction(),
			new AtBase64Function(),
			new AtHtmlFunction(),
			new AtShFunction(),
			new AtUriFunction(),
			new BuiltinsFunction(),
			new ContainsFunction(),
			new CsvFilter(),
			new DelPathsFunction(),
			new EmptyFunction(),
			new EndsWithFunction(),
			new ErrorFunction(),
			new ExplodeFunction(),
			new FromDateIso8601Function(),
			new FromEntriesFunction(),
			new FromJsonFunction(),
			new GetPathFunction(),
			new GroupByFunction(),
			new HasFunction(),
			new ImplodeFunction(),
			new IndexFunction(),
			new IndicesFunction(),
			new InfiniteFunction(),
			new IsEmptyFunction(),
			new IsInfiniteFunction(),
			new IsNanFunction(),
			new IsNormalFunction(),
			new JoinFunction(),
			new KeysFunction(),
			new KeysUnsortedFunction(),
			new LengthFunction(),
			new LTrimStrFunction(),
			new MathFunctions.AcosFunction(),
			new MathFunctions.AsinFunction(),
			new MathFunctions.AtanFunction(),
			new MathFunctions.CbrtFunction(),
			new MathFunctions.CeilFunction(),
			new MathFunctions.CosFunction(),
			new MathFunctions.CoshFunction(),
			new MathFunctions.Exp10Function(),
			new MathFunctions.Exp2Function(),
			new MathFunctions.ExpFunction(),
			new MathFunctions.Expm1Function(),
			new MathFunctions.FloorFunction(),
			new MathFunctions.Log10Function(),
			new MathFunctions.Log1pFunction(),
			new MathFunctions.Log2Function(),
			new MathFunctions.LogFunction(),
			new MathFunctions.RoundFunction(),
			new MathFunctions.SinFunction(),
			new MathFunctions.SinhFunction(),
			new MathFunctions.SqrtFunction(),
			new MathFunctions.TanFunction(),
			new MathFunctions.TanhFunction(),
			new MaxByFunction(),
			new MinByFunction(),
			new ModuleMetaFunction(),
			new NanFunction(),
			new NotFunction(),
			new NowFunction(),
			new PathFunction(),
			new PathsFunction(),
			new PowFunction(),
			new RangeFunction(),
			new ReverseFunction(),
			new RIndexFunction(),
			new RTrimStrFunction(),
			new SetPathFunction(),
			new SortByFunction(),
			new SplitFunction(),
			new StartsWithFunction(),
			new ToDateIso8601Function(),
			new ToEntriesFunction(),
			new ToJsonFunction(),
			new ToNumberFunction(),
			new ToStringFunction(),
			new TsvFilter(),
			new TypeFunction(),
			new Utf8ByteLengthFunction()
	);

	private static final List<JqLibrary> LIBRARIES = List.of(new CoreJqLibrary());

	private static final BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	/**
	 * One builtin under one of the names it registers itself as. Reading a {@link FunctionRegistration}
	 * off a class is reflection, so it is done once, when this loader is created; what a lookup is left to
	 * filter on is the version range that name is live for.
	 */
	private record Registration(FunctionSignature signature, VersionRange version, Function function) {
	}

	private final List<Registration> registrations;

	private final List<JqFunction> jqFunctions;

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	private BuiltinFunctionLoader() {
		List<Registration> registrations = new ArrayList<>();
		for (Function function : FUNCTIONS) {
			for (FunctionRegistration registration : function.getClass().getAnnotationsByType(FunctionRegistration.class))
				registrations.add(new Registration(signatureOf(registration), VersionRange.from(registration.version()), function));
		}
		this.registrations = List.copyOf(registrations);

		List<JqFunction> jqFunctions = new ArrayList<>();
		for (JqLibrary library : LIBRARIES)
			jqFunctions.addAll(library.getJqFunctions());
		this.jqFunctions = List.copyOf(jqFunctions);
	}

	/**
	 * Every Java-implemented builtin that applies to {@code jqVersion}.
	 * <p>
	 * Unlike a discovered registry, this one is closed: two builtins claiming the same
	 * {@link FunctionSignature} on the same version is a bug in this class, not an ambiguity to resolve,
	 * so it is rejected rather than silently decided.
	 */
	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		Map<FunctionSignature, Function> result = new HashMap<>();
		for (Registration registration : registrations) {
			if (!registration.version().contains(jqVersion))
				continue;
			Function previous = result.put(registration.signature(), registration.function());
			if (previous != null) {
				throw new IllegalStateException(String.format("Builtin %s on jq %s is registered by both %s and %s",
						registration.signature(), jqVersion, previous.getClass().getName(), registration.function().getClass().getName()));
			}
		}
		return result;
	}

	/**
	 * A negative {@link FunctionRegistration#nargs()} registers a variadic function, matching {@link FunctionSignature}'s null-arity convention.
	 */
	static FunctionSignature signatureOf(FunctionRegistration registration) {
		return registration.nargs() < 0 ? FunctionSignature.ofVariadic(registration.name()) : FunctionSignature.of(registration.name(), registration.nargs());
	}

	/**
	 * Every jq-language builtin that applies to {@code jqVersion}, rejecting duplicates on the same terms
	 * as {@link #getFunctions(Version)}.
	 */
	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
		Map<FunctionSignature, JqFunction> result = new HashMap<>();
		for (JqFunction definition : jqFunctions) {
			VersionRange version = definition.version();
			if (version != null && !version.contains(jqVersion))
				continue;
			JqFunction previous = result.put(definition.signature(), definition);
			if (previous != null)
				throw new IllegalStateException(String.format("Builtin %s on jq %s is defined twice",
						definition.signature(), jqVersion));
		}
		return result;
	}

	/**
	 * {@code builtins/0}, which answers with the names this loader supplies. It lives here because the
	 * registry it reports is this class's, and it is the only builtin that needs to read it.
	 * <p>
	 * The list is taken once per call site, while the call is being bound, not on every input.
	 */
	@FunctionRegistration(name = "builtins", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	private static final class BuiltinsFunction implements Function {
		private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
				TypeScheme.of(FunctionType.of(AnyType.getInstance(), ArrayType.of(StringType.getInstance()))));

		@Override
		public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
			return TYPE_SCHEMES;
		}

		@Override
		public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
			return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, false, false, arguments);
		}

		@Override
		public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
			JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
			Version jqVersion = bindCtx.getJqVersion();
			Set<FunctionSignature> signatures = new HashSet<>(INSTANCE.getFunctions(jqVersion).keySet());
			signatures.addAll(INSTANCE.getJqFunctions(jqVersion).keySet());
			List<String> builtins = new ArrayList<>(signatures.size());
			for (FunctionSignature signature : signatures)
				builtins.add(signature.toString());
			Collections.sort(builtins);

			return (scope, in, path, output) -> {
				List<JsonNode> result = new ArrayList<>(builtins.size());
				for (String builtin : builtins)
					result.add(jsonProvider.createString(builtin));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
			};
		}
	}
}
