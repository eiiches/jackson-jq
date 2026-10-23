package net.thisptr.jackson.jq.v2.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.function.FunctionOrigin;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that {@link EnvironmentBuilder#setFunctionFilter(FunctionFilter)} hides functions whatever their
 * kind (Java or jq) and source (builtins, function loaders, modules), and leaves explicit definitions alone.
 */
public class EnvironmentFunctionFilterTest {
	private static final String DOES_NOT_EXIST = "does not exist";

	private static Function constantFunction(String text) {
		return new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(jsonProvider.createString(text), UntrackedPath.getInstance());
			}
		};
	}

	private static FunctionLoader functionLoader(Map<FunctionSignature, Function> functions, Map<FunctionSignature, JqFunction> jqFunctions) {
		return new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return functions;
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return jqFunctions;
			}
		};
	}

	private static FunctionLoader javaLoader(String name, String text) {
		return functionLoader(Collections.singletonMap(FunctionSignature.of(name, 0), constantFunction(text)), Collections.emptyMap());
	}

	private static FunctionLoader jqLoader(String name, String body) {
		return functionLoader(Collections.emptyMap(),
				Collections.singletonMap(FunctionSignature.of(name, 0), JqFunction.of(name, Collections.emptyList(), body)));
	}

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6);
	}

	private static List<JsonNode> execute(Environment<JsonNode> env, String source) {
		List<JsonNode> out = new ArrayList<>();
		env.compile(source).apply(env.getJsonProvider().createNull(), out::add);
		return out;
	}

	private static List<String> texts(Environment<JsonNode> env, String source) {
		return execute(env, source).stream().map(JsonNode::asText).toList();
	}

	private static final class SourceModule implements JqModule {
		private final String source;

		SourceModule(String source) {
			this.source = source;
		}

		@Override
		public String getSourceCode() {
			return source;
		}
	}

	private static final class HybridModule implements JqModule, JavaModule {
		private final String source;
		private final Map<FunctionSignature, Function> functions;

		HybridModule(String source, Map<FunctionSignature, Function> functions) {
			this.source = source;
			this.functions = functions;
		}

		@Override
		public String getSourceCode() {
			return source;
		}

		@Override
		public Map<FunctionSignature, Function> getFunctions() {
			return functions;
		}
	}

	private static JavaModule javaModule(String name, String text) {
		Map<FunctionSignature, Function> functions = Collections.singletonMap(FunctionSignature.of(name, 0), constantFunction(text));
		return () -> functions;
	}

	/**
	 * Registered through {@code META-INF/services} by {@link #classPathFunctionsAreFilteredWithoutExtraConfiguration}.
	 */
	@FunctionRegistration(name = "from_service", nargs = 0)
	public static final class ServiceFunction implements Function {
		@Override
		public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
			JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
			return (frame, in, path, output) -> output.emit(jsonProvider.createString("service"), UntrackedPath.getInstance());
		}
	}

	// ------------------------------------------------------------------ the default

	@Test
	public void noFilterLeavesEverythingVisible() {
		Environment<JsonNode> env = builder().addFunctionLoader(javaLoader("greet", "hello")).build();

		assertThat(env.getFunctionFilter()).isSameAs(FunctionFilter.acceptAll());
		assertThat(env.getFunctionLoaders()).hasSize(2);
		assertThat(texts(env, "greet")).containsExactly("hello");
		assertThat(execute(env, "[1, 2] | map(. + 1)")).hasSize(1);
	}

	@Test
	public void theConfiguredFilterIsExposedByTheEnvironment() {
		FunctionFilter filter = FunctionFilter.denyNames(Set.of("greet"));

		assertThat(builder().setFunctionFilter(filter).build().getFunctionFilter()).isSameAs(filter);
	}

	// ------------------------------------------------------------------ builtins

	@Test
	public void aJavaBuiltinCanBeFilteredBySignature() {
		Environment<JsonNode> env = builder()
				.setFunctionFilter(FunctionFilter.denySignatures(Set.of(FunctionSignature.of("length", 0))))
				.build();

		assertThatThrownBy(() -> env.compile("[1] | length")).hasMessageContaining("Function length/0 " + DOES_NOT_EXIST);
		assertThat(execute(env, "[1] | map(. + 1)")).hasSize(1);
	}

	@Test
	public void aJqBuiltinCanBeFilteredByName() {
		Environment<JsonNode> env = builder().setFunctionFilter(FunctionFilter.denyNames(Set.of("map"))).build();

		assertThatThrownBy(() -> env.compile("[1] | map(. + 1)")).hasMessageContaining("Function map/1 " + DOES_NOT_EXIST);
		assertThat(execute(env, "[1] | length")).hasSize(1);
	}

	@Test
	public void filteringOneArityKeepsTheOthers() {
		Environment<JsonNode> env = builder()
				.setFunctionFilter(FunctionFilter.denySignatures(Set.of(FunctionSignature.of("any", 0))))
				.build();

		assertThatThrownBy(() -> env.compile("[true] | any")).hasMessageContaining("Function any/0 " + DOES_NOT_EXIST);
		assertThat(texts(env, "[true] | any(.)")).containsExactly("true");
	}

	@Test
	public void anAllowListWrittenAsALambdaWorks() {
		Set<String> allowed = Set.of("length", "greet");
		Environment<JsonNode> env = builder()
				.addFunctionLoader(javaLoader("greet", "hello"))
				.setFunctionFilter(ctx -> allowed.contains(ctx.getSignature().name()))
				.build();

		assertThat(texts(env, "greet")).containsExactly("hello");
		assertThat(execute(env, "[1] | length")).hasSize(1);
		assertThatThrownBy(() -> env.compile("[1] | keys")).hasMessageContaining("Function keys/0 " + DOES_NOT_EXIST);
	}

	// ------------------------------------------------------------------ function loaders

	@Test
	public void javaAndJqLoaderFunctionsAreFilteredAlike() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(javaLoader("java_fn", "java"))
				.addFunctionLoader(jqLoader("jq_fn", "\"jq\""))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("java_fn", "jq_fn")))
				.build();

		assertThatThrownBy(() -> env.compile("java_fn")).hasMessageContaining("Function java_fn/0 " + DOES_NOT_EXIST);
		assertThatThrownBy(() -> env.compile("jq_fn")).hasMessageContaining("Function jq_fn/0 " + DOES_NOT_EXIST);
	}

	@Test
	public void theFilterAppliesWhetherSetBeforeOrAfterAddingALoader() {
		FunctionFilter filter = FunctionFilter.denyNames(Set.of("greet"));
		Environment<JsonNode> before = builder().setFunctionFilter(filter).addFunctionLoader(javaLoader("greet", "hello")).build();
		Environment<JsonNode> after = builder().addFunctionLoader(javaLoader("greet", "hello")).setFunctionFilter(filter).build();

		assertThatThrownBy(() -> before.compile("greet")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> after.compile("greet")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void theFilterSurvivesClearFunctionLoaders() {
		Environment<JsonNode> env = builder()
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("greet")))
				.clearFunctionLoaders()
				.addFunctionLoader(javaLoader("greet", "hello"))
				.addFunctionLoader(javaLoader("other", "kept"))
				.build();

		assertThatThrownBy(() -> env.compile("greet")).hasMessageContaining(DOES_NOT_EXIST);
		assertThat(texts(env, "other")).containsExactly("kept");
	}

	@Test
	public void aLaterFilterReplacesAnEarlierOne() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(javaLoader("greet", "hello"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("greet")))
				.setFunctionFilter(FunctionFilter.acceptAll())
				.build();

		assertThat(texts(env, "greet")).containsExactly("hello");
	}

	@Test
	public void aVariadicLoaderFunctionIsOnlyMatchedByAVariadicSignatureOrItsName() {
		FunctionLoader loader = functionLoader(
				Collections.singletonMap(FunctionSignature.ofVariadic("greet"), constantFunction("variadic")), Collections.emptyMap());
		Environment<JsonNode> arityDenied = builder()
				.addFunctionLoader(loader)
				.setFunctionFilter(FunctionFilter.denySignatures(Set.of(FunctionSignature.of("greet", 0))))
				.build();
		Environment<JsonNode> variadicDenied = builder()
				.addFunctionLoader(loader)
				.setFunctionFilter(FunctionFilter.denySignatures(Set.of(FunctionSignature.ofVariadic("greet"))))
				.build();
		Environment<JsonNode> nameDenied = builder()
				.addFunctionLoader(loader)
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("greet")))
				.build();

		assertThat(texts(arityDenied, "greet")).containsExactly("variadic");
		assertThatThrownBy(() -> variadicDenied.compile("greet")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> nameDenied.compile("greet")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void classPathFunctionsAreFilteredWithoutExtraConfiguration(@TempDir Path tempDir) throws Exception {
		Path services = Files.createDirectories(tempDir.resolve("META-INF").resolve("services"));
		Files.writeString(services.resolve(Function.class.getName()), ServiceFunction.class.getName() + "\n");

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { tempDir.toUri().toURL() }, getClass().getClassLoader())) {
			Environment<JsonNode> open = builder().addFunctionLoader(new ClassPathFunctionLoader(classLoader)).build();
			Environment<JsonNode> filtered = builder()
					.addFunctionLoader(new ClassPathFunctionLoader(classLoader))
					.setFunctionFilter(FunctionFilter.denyNames(Set.of("from_service")))
					.build();

			assertThat(texts(open, "from_service")).containsExactly("service");
			assertThatThrownBy(() -> filtered.compile("from_service")).hasMessageContaining("Function from_service/0 " + DOES_NOT_EXIST);
		}
	}

	// ------------------------------------------------------------------ the filter reaches inside jq definitions

	@Test
	public void aLoaderJqFunctionDependingOnAFilteredFunctionFails() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(jqLoader("outer", "inner"))
				.addFunctionLoader(javaLoader("inner", "x"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("inner")))
				.build();

		assertThatThrownBy(() -> env.compile("outer")).hasMessageContaining("Function inner/0 " + DOES_NOT_EXIST);
	}

	@Test
	public void aJqBuiltinFailsWhenOneOfItsDependenciesIsFiltered() {
		// "values" is defined in jq on top of numbers, strings, ...
		Environment<JsonNode> open = builder().build();
		Environment<JsonNode> filtered = builder().setFunctionFilter(FunctionFilter.denyNames(Set.of("numbers"))).build();

		assertThat(execute(open, "[1, null] | map(values)")).hasSize(1);
		assertThatThrownBy(() -> filtered.compile("[1, null] | map(values)")).hasMessageContaining("Function numbers/0 " + DOES_NOT_EXIST);
	}

	// ------------------------------------------------------------------ explicit definitions are not filtered

	@Test
	public void explicitDefinitionsAreNeverFiltered() {
		FunctionSignature declared = FunctionSignature.of("declared", 0);
		Environment<JsonNode> env = builder()
				.defineFunction(FunctionSignature.of("defined", 0), constantFunction("java"))
				.defineJqFunction(JqFunction.of("defined_jq", Collections.emptyList(), "\"jq\""))
				.declareFunction(declared)
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("defined", "defined_jq", "declared")))
				.build();
		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder().setFunction(declared, constantFunction("declared")).build();
		List<JsonNode> out = new ArrayList<>();

		assertThat(texts(env, "defined")).containsExactly("java");
		assertThat(texts(env, "defined_jq")).containsExactly("jq");
		env.compile("declared").withRuntimeBindings(bindings).apply(env.getJsonProvider().createNull(), out::add);
		assertThat(out).extracting(JsonNode::asText).containsExactly("declared");
	}

	@Test
	public void anExplicitDefinitionStillSeesUnfilteredFunctionsOnly() {
		Environment<JsonNode> env = builder()
				.defineJqFunction(JqFunction.of("uses_length", Collections.emptyList(), "length"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("length")))
				.build();

		assertThatThrownBy(() -> env.compile("[1] | uses_length")).hasMessageContaining("Function length/0 " + DOES_NOT_EXIST);
	}

	// ------------------------------------------------------------------ modules

	@Test
	public void aRegisteredJavaModuleIsFilteredForImportAndInclude() {
		Environment<JsonNode> open = builder().registerModule("lib", javaModule("greet", "hello")).build();
		Environment<JsonNode> filtered = builder()
				.registerModule("lib", javaModule("greet", "hello"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("greet")))
				.build();

		assertThat(texts(open, "import \"lib\" as lib; lib::greet")).containsExactly("hello");
		assertThat(texts(open, "include \"lib\"; greet")).containsExactly("hello");
		assertThatThrownBy(() -> filtered.compile("import \"lib\" as lib; lib::greet")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> filtered.compile("include \"lib\"; greet")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void aRegisteredJqModuleExportsOnlyAcceptedFunctions() {
		Environment<JsonNode> env = builder()
				.registerModule("lib", new SourceModule("def hidden: \"h\"; def shown: \"s\";"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("hidden")))
				.build();

		assertThat(texts(env, "import \"lib\" as lib; lib::shown")).containsExactly("s");
		assertThatThrownBy(() -> env.compile("import \"lib\" as lib; lib::hidden")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> env.compile("include \"lib\"; hidden")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void aJqModuleStillCallsItsOwnFilteredDefinitions() {
		Environment<JsonNode> env = builder()
				.registerModule("lib", new SourceModule("def hidden: \"h\"; def shown: hidden + \"s\";"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("hidden")))
				.build();

		assertThat(texts(env, "import \"lib\" as lib; lib::shown")).containsExactly("hs");
	}

	@Test
	public void aJqModuleCannotCallAFilteredBuiltin() {
		Environment<JsonNode> env = builder()
				.registerModule("lib", new SourceModule("def doubled: map(. * 2);"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("map")))
				.build();

		assertThatThrownBy(() -> env.compile("import \"lib\" as lib; [1] | lib::doubled"))
				.hasMessageContaining("Function map/1 " + DOES_NOT_EXIST);
	}

	@Test
	public void aHybridModuleFiltersBothKindsOfExport() {
		HybridModule hybrid = new HybridModule("def jq: java; def kept: \"kept\";",
				Map.of(FunctionSignature.of("java", 0), constantFunction("from-java")));
		Environment<JsonNode> env = builder()
				.registerModule("lib", hybrid)
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("java")))
				.build();

		// the module still uses its own Java function internally; only what it exports is filtered
		assertThat(texts(env, "import \"lib\" as lib; lib::jq")).containsExactly("from-java");
		assertThat(texts(env, "import \"lib\" as lib; lib::kept")).containsExactly("kept");
		assertThatThrownBy(() -> env.compile("import \"lib\" as lib; lib::java")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void includeModuleIsFiltered() {
		Environment<JsonNode> env = builder()
				.includeModule(javaModule("hidden", "h"))
				.includeModule(new SourceModule("def shown: \"s\"; def hidden_jq: \"h\";"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("hidden", "hidden_jq")))
				.build();

		assertThat(texts(env, "shown")).containsExactly("s");
		assertThatThrownBy(() -> env.compile("hidden")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> env.compile("hidden_jq")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void importModuleIsFiltered() {
		Environment<JsonNode> env = builder()
				.importModule(javaModule("hidden", "h"), "java_lib")
				.importModule(new SourceModule("def shown: \"s\"; def hidden_jq: \"h\";"), "jq_lib")
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("hidden", "hidden_jq")))
				.build();

		assertThat(texts(env, "jq_lib::shown")).containsExactly("s");
		assertThatThrownBy(() -> env.compile("java_lib::hidden")).hasMessageContaining(DOES_NOT_EXIST);
		assertThatThrownBy(() -> env.compile("jq_lib::hidden_jq")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void aModuleIsUnfilteredWithoutAFilter() {
		Environment<JsonNode> env = builder()
				.registerModule("lib", new SourceModule("def shown: \"s\";"))
				.includeModule(javaModule("included", "i"))
				.importModule(javaModule("imported", "m"), "alias")
				.build();

		assertThat(texts(env, "import \"lib\" as lib; lib::shown")).containsExactly("s");
		assertThat(texts(env, "included")).containsExactly("i");
		assertThat(texts(env, "alias::imported")).containsExactly("m");
	}

	/**
	 * {@code modulemeta} is informational: it still lists a filtered signature under {@code defs}, but that
	 * signature cannot be called.
	 */
	@Test
	public void modulemetaIsNotFiltered() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.registerModule("lib", new SourceModule("module {name: \"lib\"}; def hidden: 1;"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("hidden")))
				.build();

		JsonNode meta = execute(env, "\"lib\" | modulemeta").get(0);

		assertThat(meta.path("name").asText()).isEqualTo("lib");
		assertThat(meta.path("defs")).extracting(JsonNode::asText).containsExactly("hidden/0");
		assertThatThrownBy(() -> env.compile("import \"lib\" as lib; lib::hidden")).hasMessageContaining(DOES_NOT_EXIST);
	}

	@Test
	public void filteringDoesNotAddFunctionLoadersToTheEnvironment() {
		Environment<JsonNode> open = builder().addFunctionLoader(javaLoader("greet", "hello")).build();
		Environment<JsonNode> filtered = builder()
				.addFunctionLoader(javaLoader("greet", "hello"))
				.setFunctionFilter(FunctionFilter.denyNames(Set.of("other")))
				.build();

		assertThat(filtered.getFunctionLoaders()).hasSameSizeAs(open.getFunctionLoaders());
	}

	@Test
	public void filterByModulePathRejectsOnlyFunctionsFromMatchingPath() {
		Environment<JsonNode> env = builder()
				.registerModule("blocked/mod", new SourceModule("def helper: \"blocked\";"))
				.registerModule("allowed/mod", new SourceModule("def helper: \"allowed\";"))
				.setFunctionFilter(ctx -> !(ctx.getOrigin() == FunctionOrigin.PATH_IMPORT && ctx.getLocator().equals("blocked/mod")))
				.build();

		assertThat(texts(env, "import \"allowed/mod\" as a; a::helper")).containsExactly("allowed");
		assertThatThrownBy(() -> env.compile("import \"blocked/mod\" as b; b::helper"))
				.hasMessageContaining("Function b::helper/0 " + DOES_NOT_EXIST);
	}

	@Test
	public void filterDistinguishesPathlessFunctionsFromModuleFunctions() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(javaLoader("helper", "builtin_helper"))
				.registerModule("my/mod", new SourceModule("def helper: \"mod_helper\";"))
				.setFunctionFilter(ctx -> ctx.getOrigin() == FunctionOrigin.FUNCTION_LOADER)
				.build();

		assertThat(texts(env, "helper")).containsExactly("builtin_helper");
		assertThatThrownBy(() -> env.compile("import \"my/mod\" as m; m::helper"))
				.hasMessageContaining("Function m::helper/0 " + DOES_NOT_EXIST);
	}

	@Test
	public void filterByEnvImportAlias() {
		Environment<JsonNode> env = builder()
				.importModule(new SourceModule("def fn: \"blocked\";"), "blocked_alias")
				.importModule(new SourceModule("def fn: \"allowed\";"), "allowed_alias")
				.setFunctionFilter(ctx -> !(ctx.getOrigin() == FunctionOrigin.ENV_IMPORT && ctx.getLocator().equals("blocked_alias")))
				.build();

		assertThat(texts(env, "allowed_alias::fn")).containsExactly("allowed");
		assertThatThrownBy(() -> env.compile("blocked_alias::fn"))
				.hasMessageContaining("Function blocked_alias::fn/0 " + DOES_NOT_EXIST);
	}
}
