package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what the compiler does with an environment's module loaders: which one is asked, in what
 * order, what a failure from one means for the others, and what happens to a module once it is in
 * hand -- compiled once, and never twice in a cycle.
 */
public class ModuleResolverTest {

	/**
	 * Serves jq source and data under names of its own, like a loader reading files off a search
	 * root. Names are {@code /}-separated paths, so a module can resolve an import relative to
	 * itself the way a filesystem one does.
	 */
	private static final class SourceLoader implements ModuleLoader<JsonNode> {
		private final String root;
		private final Map<String, String> sources = new HashMap<>();
		private final Map<String, Integer> datas = new HashMap<>();
		private final List<String> requested = new ArrayList<>();

		SourceLoader(String root) {
			this.root = root;
		}

		SourceLoader put(String path, String source) {
			sources.put(root + "/" + path, source);
			return this;
		}

		SourceLoader putData(String path, int data) {
			datas.put(root + "/" + path, data);
			return this;
		}

		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			requested.add(path);
			return moduleAt(root + "/" + path, path);
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			requested.add(path);
			return dataAt(root + "/" + path, path, Jackson2JsonProvider.getInstance());
		}

		SourceModule moduleAt(String name, String path) {
			String source = sources.get(name);
			if (source == null)
				throw new ModuleNotFoundException(path);
			return new SourceModule(this, name, source);
		}

		<T> T dataAt(String name, String path, JsonProvider<T> jsonProvider) {
			Integer data = datas.get(name);
			if (data == null)
				throw new ModuleNotFoundException(path);
			return jsonProvider.createNumber(data);
		}
	}

	private static final class SourceModule implements JqModule {
		private final SourceLoader owner;
		private final String name;
		private final String source;
		private final Map<String, Module> bundledModules;
		private final Map<String, Module> relativeModules;
		private final Map<String, Integer> bundledData;

		SourceModule(SourceLoader owner, String name, String source) {
			this(owner, name, source, Map.of(), Map.of(), Map.of());
		}

		SourceModule(SourceLoader owner, String name, String source, Map<String, Module> bundledModules, Map<String, Module> relativeModules) {
			this(owner, name, source, bundledModules, relativeModules, Map.of());
		}

		SourceModule(SourceLoader owner, String name, String source, Map<String, Module> bundledModules, Map<String, Module> relativeModules,
				Map<String, Integer> bundledData) {
			this.owner = owner;
			this.name = name;
			this.source = source;
			this.bundledModules = Map.copyOf(bundledModules);
			this.relativeModules = Map.copyOf(relativeModules);
			this.bundledData = Map.copyOf(bundledData);
		}

		@Override
		public String getSourceCode() {
			return source;
		}

		/**
		 * {@code /first/lib/a} + {@code "./"} + {@code "b"} is {@code /first/lib/b}.
		 */
		private String resolve(String importPath, String searchPath) {
			return Objects.requireNonNull(Paths.get(name).getParent()).resolve(searchPath).resolve(importPath).normalize().toString();
		}

		@Override
		public Module loadModule(String importPath, @Nullable String searchPath) {
			if (searchPath == null) {
				Module bundled = bundledModules.get(importPath);
				if (bundled != null)
					return bundled;
				throw new ModuleNotFoundException(importPath);
			}
			Module relative = relativeModules.get(importPath);
			if (relative != null)
				return relative;
			return owner.moduleAt(resolve(importPath, searchPath), importPath);
		}

		@Override
		public <T> T loadData(String importPath, @Nullable String searchPath, JsonProvider<T> jsonProvider) {
			if (searchPath == null) {
				Integer bundled = bundledData.get(importPath);
				if (bundled != null)
					return jsonProvider.createNumber(bundled);
				throw new ModuleNotFoundException(importPath);
			}
			return owner.dataAt(resolve(importPath, searchPath), importPath, jsonProvider);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			return o instanceof SourceModule other && name.equals(other.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static final class HybridModule implements JavaModule, JqModule {
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


		@Override
		public String toString() {
			return "hybrid";
		}
	}

	private static final class SingleModuleLoader implements ModuleLoader<JsonNode> {
		private final Module module;

		SingleModuleLoader(Module module) {
			this.module = module;
		}

		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			return module;
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}
	}

	/**
	 * Has nothing, like a search root the module simply isn't under.
	 */
	private static final class MissingModuleLoader implements ModuleLoader<JsonNode> {
		private final List<String> requested = new ArrayList<>();

		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			requested.add(path);
			throw new ModuleNotFoundException(path);
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			requested.add(path);
			throw new ModuleNotFoundException(path);
		}
	}

	/**
	 * Resolved the path, then failed on it -- an unreadable module file, say.
	 */
	private static final class FailingModuleLoader implements ModuleLoader<JsonNode> {
		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			throw new JsonQueryException("failed to load module " + path + ": boom");
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			throw new JsonQueryException("failed to load data " + path + ": boom");
		}
	}

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders();
	}

	/**
	 * Compared as text: which node class the compiler produced is not what these tests are about.
	 */
	private static List<String> run(Environment<JsonNode> env, String query) {
		JsonQuery<JsonNode> expr = env.compile(query);
		List<String> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), value -> actual.add(value.toString()));
		return actual;
	}

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private static Function constantFunction(int value) {
		return new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				return (context, in, path, output) -> output.emit(bindCtx.getJsonProvider().createNumber(value), UntrackedPath.getInstance());
			}
		};
	}

	@Test
	public void testMissedLoaderFallsThroughToNextLoader() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new MissingModuleLoader())
				.addModuleLoader(new SourceLoader("/second").put("foo", "def one: 1;"))
				.build();

		assertThat(run(env, "import \"foo\" as foo; foo::one")).containsExactly("1");
	}

	@Test
	public void testFailingLoaderAbortsTheSearch() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new FailingModuleLoader())
				.addModuleLoader(new SourceLoader("/second").put("foo", "def one: 1;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::one"))
				.isInstanceOf(JsonQueryException.class)
				.isNotInstanceOf(ModuleNotFoundException.class)
				.hasMessage("failed to load module foo: boom");
	}

	@Test
	public void testAllLoadersMissing() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new MissingModuleLoader())
				.addModuleLoader(new MissingModuleLoader())
				.build();

		assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::one"))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
		assertThatThrownBy(() -> env.compile("import \"foo\" as $foo; $foo::foo"))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
	}

	/**
	 * A module's non-relative import is nobody's in particular, so it walks the whole list in order
	 * -- reaching a loader other than the one the importing module came from.
	 */
	@Test
	public void testNonRelativeImportInsideModuleWalksEveryLoader() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first").put("a", "import \"b\" as b; def one: b::two - 1;"))
				.addModuleLoader(new SourceLoader("/second").put("b", "def two: 2;"))
				.build();

		assertThat(run(env, "import \"a\" as a; a::one")).containsExactly("1");
	}

	/**
	 * A relative import means "next to me", which only the importing module can answer -- so it
	 * answers, and no loader is consulted for it at all.
	 */
	@Test
	public void testRelativeImportIsResolvedByTheModuleItself() {
		MissingModuleLoader other = new MissingModuleLoader();
		SourceLoader producer = new SourceLoader("/first")
				.put("lib/a", "import \"b\" as b {search: \"./\"}; def one: b::two - 1;")
				.put("lib/b", "def two: 2;");
		Environment<JsonNode> env = builder()
				.addModuleLoader(other)
				.addModuleLoader(producer)
				.build();

		assertThat(run(env, "import \"lib/a\" as a; a::one")).containsExactly("1");
		// "lib/a" itself went through the list; the relative "b" inside it never did.
		assertThat(other.requested).containsExactly("lib/a");
		assertThat(producer.requested).containsExactly("lib/a");
	}

	/**
	 * {@code import "x" as $d {search: "./"}} -- the data counterpart, resolved by the module too.
	 */
	@Test
	public void testRelativeDataImportIsResolvedByTheModuleItself() {
		MissingModuleLoader other = new MissingModuleLoader();
		Environment<JsonNode> env = builder()
				.addModuleLoader(other)
				.addModuleLoader(new SourceLoader("/first")
						.put("lib/a", "import \"nums\" as $nums {search: \"./\"}; def one: $nums::nums;")
						.putData("lib/nums", 7))
				.build();

		assertThat(run(env, "import \"lib/a\" as a; a::one")).containsExactly("7");
		assertThat(other.requested).containsExactly("lib/a");
	}

	/**
	 * An environment can be handed either kind of module. jq source is compiled the first time a
	 * query calls into it, and not at all if no query does.
	 */
	@Test
	public void testImportedJqModuleFromEnvironmentIsCompiledOnUse() {
		SourceLoader loader = new SourceLoader("/first").put("helper", "def three: 3;");
		SourceModule module = loader.moduleAt("/first/helper", "helper");

		Environment<JsonNode> env = builder()
				.importModule(module, "m")
				.build();

		assertThat(run(env, "m::three")).containsExactly("3");
		// Nothing referenced it, so nothing compiled it.
		assertThat(run(env, "1 + 1")).containsExactly("2");
	}

	/**
	 * A module's jq source is the only place a definition it exports can state its signature, and this
	 * is what carries the statement across the boundary: an export is a {@link Function}, and a caller
	 * is typed from what that publishes. Without the statement the caller is told {@code ANY}, whatever
	 * the body does.
	 */
	@Test
	public void testImportedDefinitionPublishesTheTypeItStates() {
		SourceLoader loader = new SourceLoader("/first")
				.put("stated", "#jackson-jq:type () => (STRING -> INT)\ndef n: length;")
				.put("unstated", "def n: length;");

		Environment<JsonNode> env = builder()
				.importModule(loader.moduleAt("/first/stated", "stated"), "stated")
				.importModule(loader.moduleAt("/first/unstated", "unstated"), "unstated")
				.build();

		assertThat(env.compile("stated::n", strict(StringType.getInstance())).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(env.compile("unstated::n", strict(StringType.getInstance())).getType().outputType())
				.isSameAs(AnyType.getInstance());
		assertThat(run(env, "\"abc\" | stated::n")).containsExactly("3");
	}

	/**
	 * The statement is checked at the call, so a module states the input its definition is written for
	 * rather than leaving a caller to find out at runtime.
	 */
	@Test
	public void testImportedDefinitionRejectsAnInputItDoesNotState() {
		SourceLoader loader = new SourceLoader("/first")
				.put("stated", "#jackson-jq:type () => (STRING -> INT)\ndef n: length;");

		Environment<JsonNode> env = builder()
				.importModule(loader.moduleAt("/first/stated", "stated"), "stated")
				.build();

		assertThatThrownBy(() -> env.compile("stated::n", strict(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	public void testHybridModuleCombinesJavaAndJqFunctions() {
		FunctionSignature javaHelper = FunctionSignature.of("java_helper", 0);
		HybridModule module = new HybridModule(
				"module {\"kind\": \"hybrid\"}; def from_jq: java_helper;",
				Collections.singletonMap(javaHelper, constantFunction(7)));
		Environment<JsonNode> loaderEnvironment = builder()
				.addModuleLoader(new SingleModuleLoader(module))
				.build();
		Environment<JsonNode> importedEnvironment = builder()
				.importModule(module, "hybrid")
				.build();

		assertThat(run(loaderEnvironment, "import \"hybrid\" as hybrid; [hybrid::java_helper, hybrid::from_jq]")).containsExactly("[7,7]");
		assertThat(run(importedEnvironment, "[hybrid::java_helper, hybrid::from_jq]")).containsExactly("[7,7]");

		JavaModule materialized = new ModuleResolver<>(loaderEnvironment).materialize(module);
		assertThat(materialized.getModuleMeta().getMetadata(Jackson2JsonProvider.getInstance()))
				.containsEntry("kind", Jackson2JsonProvider.getInstance().createString("hybrid"));
	}

	@Test
	public void testHybridModuleRejectsDuplicateFunctionSignature() {
		FunctionSignature duplicate = FunctionSignature.of("duplicate", 0);
		HybridModule module = new HybridModule(
				"def duplicate: 1;",
				Collections.singletonMap(duplicate, constantFunction(2)));

		assertThatThrownBy(() -> new ModuleResolver<>(builder().build()).materialize(module))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("module hybrid defines function duplicate/0 in both Java and jq source");
	}

	@Test
	public void testHybridModuleAllowsExactAndVariadicSignaturesToCoexist() {
		HybridModule module = new HybridModule(
				"def shared: 1;",
				Collections.singletonMap(FunctionSignature.ofVariadic("shared"), constantFunction(2)));
		Environment<JsonNode> env = builder()
				.importModule(module, "hybrid")
				.build();

		assertThat(run(env, "[hybrid::shared, hybrid::shared(0)]")).containsExactly("[1,2]");
	}

	@Test
	public void testIncludeExposesJavaModuleFunctionsWithoutAQualifier() {
		JavaModule module = () -> Map.of(
				FunctionSignature.of("selected", 0), constantFunction(1),
				FunctionSignature.ofVariadic("selected"), constantFunction(2));
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SingleModuleLoader(module))
				.build();

		assertThat(run(env, "include \"helpers\"; [selected, selected(0)]")).containsExactly("[1,2]");
	}

	@Test
	public void testLaterIncludeShadowsEarlierInclude() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first")
						.put("one", "def selected: 1;")
						.put("two", "def selected: 2;"))
				.build();

		assertThat(run(env, "include \"one\"; include \"two\"; selected")).containsExactly("2");
	}

	@Test
	public void testLocalDefinitionShadowsIncludeWhichShadowsEnvironment() {
		FunctionSignature declared = FunctionSignature.of("declared", 0);
		FunctionSignature defined = FunctionSignature.of("defined", 0);
		JavaModule module = () -> Map.of(
				declared, constantFunction(2),
				defined, constantFunction(4),
				FunctionSignature.of("length", 0), constantFunction(6));
		Environment<JsonNode> env = builder()
				.declareFunction(declared)
				.defineFunction(defined, constantFunction(3))
				.addModuleLoader(new SingleModuleLoader(module))
				.build();

		assertThat(run(env, "include \"helpers\"; [declared, defined, length]")).containsExactly("[2,4,6]");
		assertThat(run(env, "include \"helpers\"; def defined: 5; defined")).containsExactly("5");
	}

	@Test
	public void testRelativeImportFromTopLevelIsRejected() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first").put("b", "def two: 2;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"b\" as b {search: \"./\"}; b::two"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("can only be overriden from imported modules");
	}

	@Test
	public void testAbsoluteImportPathIsRejected() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first").put("b", "def two: 2;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"/first/b\" as b; b::two"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("must be relative");
	}

	/**
	 * A cycle that no single loader could see: a is served by one loader, b by another.
	 */
	@Test
	public void testCycleAcrossTwoLoadersIsReported() {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first").put("a", "import \"b\" as b; def one: b::two;"))
				.addModuleLoader(new SourceLoader("/second").put("b", "import \"a\" as a; def two: a::one;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"a\" as a; a::one"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("imported recursively");
	}

	/**
	 * The diamond: two modules import the same third one. It is read once per compilation, and both
	 * see the same compiled module.
	 */
	@Test
	public void testSameModuleImportedTwiceIsCompiledOnce() {
		SourceLoader loader = new SourceLoader("/first")
				.put("a", "import \"c\" as c; def one: c::three;")
				.put("b", "import \"c\" as c; def two: c::three;")
				.put("c", "def three: 3;");
		Environment<JsonNode> env = builder().addModuleLoader(loader).build();

		assertThat(run(env, "import \"a\" as a; import \"b\" as b; [a::one, b::two]")).containsExactly("[3,3]");
		assertThat(loader.requested).containsExactly("a", "c", "b", "c");
		assertThat(env.compile("import \"c\" as c; c::three")).isNotNull();
	}

	@Test
	public void testBundledModuleIsVisibleOnlyToItsImporter() {
		SourceLoader loader = new SourceLoader("/first");
		JavaModule yaml = () -> Map.of(FunctionSignature.of("from_yaml", 0), constantFunction(7));
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"import \"jackson-jq/yaml\" as yaml; def read_yaml: yaml::from_yaml;",
				Map.of("jackson-jq/yaml", yaml), Map.of());
		SourceModule unrelated = new SourceModule(loader, "/first/unrelated",
				"import \"jackson-jq/yaml\" as yaml; def read_yaml: yaml::from_yaml;");
		Environment<JsonNode> env = builder()
				.registerModule("wrapper", wrapper)
				.registerModule("unrelated", unrelated)
				.build();

		assertThat(run(env, "import \"wrapper\" as w; w::read_yaml")).containsExactly("7");
		assertThatThrownBy(() -> env.compile("import \"jackson-jq/yaml\" as yaml; yaml::from_yaml"))
				.isInstanceOf(ModuleNotFoundException.class);
		assertThatThrownBy(() -> env.compile("import \"unrelated\" as u; u::read_yaml"))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testBundledModuleTakesPrecedenceOverEnvironmentRegistration() {
		SourceLoader loader = new SourceLoader("/first");
		JavaModule bundled = () -> Map.of(FunctionSignature.of("value", 0), constantFunction(1));
		JavaModule registered = () -> Map.of(FunctionSignature.of("value", 0), constantFunction(2));
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"import \"dependency\" as dep; def value: dep::value;",
				Map.of("dependency", bundled), Map.of());
		Environment<JsonNode> env = builder()
				.registerModule("wrapper", wrapper)
				.registerModule("dependency", registered)
				.build();

		assertThat(run(env, "import \"wrapper\" as w; w::value")).containsExactly("1");
		assertThat(run(env, "import \"dependency\" as dep; dep::value")).containsExactly("2");
	}

	@Test
	public void testModuleLocalDataTakesPrecedenceWithoutLeaking() {
		SourceLoader loader = new SourceLoader("/first").putData("payload", 2);
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"import \"payload\" as $p; def value: $p::p;",
				Map.of(), Map.of(), Map.of("payload", 1));
		Environment<JsonNode> env = builder().registerModule("wrapper", wrapper).addModuleLoader(loader).build();

		assertThat(run(env, "import \"wrapper\" as w; w::value")).containsExactly("1");
		assertThat(run(env, "import \"payload\" as $p; $p::p")).containsExactly("2");
	}

	@Test
	public void testSameModuleAndLocalDependenciesWithDifferentJsonProviders() {
		SourceLoader owner = new SourceLoader("/first");
		SourceModule child = new SourceModule(owner, "/first/child", "def value: 40;");
		SourceModule wrapper = new SourceModule(owner, "/first/wrapper",
				"import \"child\" as child; import \"payload\" as $p; def value: child::value + $p::p;",
				Map.of("child", child), Map.of(), Map.of("payload", 2));
		String query = "import \"wrapper\" as w; w::value";

		Environment<JsonNode> jackson = builder().registerModule("wrapper", wrapper).build();
		assertThat(run(jackson, query)).containsExactly("42");

		GsonJsonProvider gsonProvider = GsonJsonProvider.getInstance();
		Environment<JsonElement> gson = EnvironmentBuilder.withDefaultLoaders(gsonProvider, Versions.JQ_1_6)
				.registerModule("wrapper", wrapper)
				.build();
		assertThat(gson.compile(query).apply(gsonProvider.createNull()))
				.containsExactly(gsonProvider.createNumber(42));
	}

	@Test
	public void testMissingPlainDataFallsThroughButMissingRelativeDataDoesNot() {
		SourceLoader local = new SourceLoader("/first");
		SourceLoader dataLoader = new SourceLoader("/second").putData("payload", 2);
		SourceModule plain = new SourceModule(local, "/first/plain",
				"import \"payload\" as $p; def value: $p::p;");
		SourceModule relative = new SourceModule(local, "/first/relative",
				"import \"payload\" as $p {search: \"./\"}; def value: $p::p;");
		Environment<JsonNode> env = builder()
				.registerModule("plain", plain)
				.registerModule("relative", relative)
				.addModuleLoader(dataLoader)
				.build();

		assertThat(run(env, "import \"plain\" as p; p::value")).containsExactly("2");
		assertThatThrownBy(() -> env.compile("import \"relative\" as r; r::value"))
				.isInstanceOf(ModuleNotFoundException.class);
		assertThat(dataLoader.requested).containsExactly("payload");
	}

	@Test
	public void testImportedModuleDoesNotInheritItsParentBundles() {
		SourceLoader loader = new SourceLoader("/first");
		JavaModule yaml = () -> Map.of(FunctionSignature.of("from_yaml", 0), constantFunction(7));
		SourceModule child = new SourceModule(loader, "/first/child",
				"import \"jackson-jq/yaml\" as yaml; def value: yaml::from_yaml;");
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"import \"child\" as child; def value: child::value;",
				Map.of("child", child, "jackson-jq/yaml", yaml), Map.of());
		JavaModule fallback = () -> Map.of(FunctionSignature.of("value", 0), constantFunction(9));
		Environment<JsonNode> env = builder().registerModule("wrapper", wrapper).registerModule("child", fallback).build();

		assertThatThrownBy(() -> env.compile("import \"wrapper\" as w; w::value"))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testRelativeJavaModuleIsVisibleOnlyToItsImporter() {
		SourceLoader loader = new SourceLoader("/first");
		JavaModule primitives = () -> Map.of(FunctionSignature.of("value_from_impl", 0), constantFunction(3));
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"include \"impl\" {search: \"./\"}; def value: value_from_impl;",
				Map.of(), Map.of("impl", primitives));
		Environment<JsonNode> env = builder().registerModule("wrapper", wrapper).build();

		assertThat(run(env, "import \"wrapper\" as w; w::value")).containsExactly("3");
		assertThatThrownBy(() -> env.compile("import \"impl\" as impl; impl::value_from_impl"))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testModuleMetaInspectsSourceWithoutResolvingDependencies() {
		SourceLoader loader = new SourceLoader("/first");
		SourceModule module = new SourceModule(loader, "/first/broken",
				"module {version: \"1\"}; import \"missing\" as dep; include \"also_missing\"; def value: dep::value;");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.registerModule("broken", module).build();

		JsonNode metadata = env.compile("\"broken\" | modulemeta").apply(NullNode.getInstance()).get(0);
		assertThat(metadata.path("version").asText()).isEqualTo("1");
		assertThat(metadata.path("deps").get(0).path("relpath").asText()).isEqualTo("missing");
		assertThat(metadata.path("deps").get(1).path("relpath").asText()).isEqualTo("also_missing");
		assertThat(metadata.path("deps").get(1).has("as")).isFalse();
		assertThat(metadata.path("defs").get(0).asText()).isEqualTo("value/0");
		assertThatThrownBy(() -> env.compile("import \"broken\" as b; b::value"))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testModuleMetaUsesCallersPrivateModules() {
		SourceLoader loader = new SourceLoader("/first");
		SourceModule privateModule = new SourceModule(loader, "/first/private", "def hidden: 1;");
		SourceModule wrapper = new SourceModule(loader, "/first/wrapper",
				"def inspect: \"private\" | modulemeta;",
				Map.of("private", privateModule), Map.of());
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.registerModule("wrapper", wrapper).build();

		JsonNode metadata = env.compile("import \"wrapper\" as w; w::inspect").apply(NullNode.getInstance()).get(0);
		assertThat(metadata.path("defs").get(0).asText()).isEqualTo("hidden/0");
		assertThatThrownBy(() -> env.compile("\"private\" | modulemeta").apply(NullNode.getInstance()))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testModuleMetaReportsJavaAndHybridFunctions() {
		ModuleMeta customMetadata = new ModuleMeta() {
			@Override
			public <T> Map<String, T> getMetadata(JsonProvider<T> jsonProvider) {
				return Map.of("kind", jsonProvider.createString("java"));
			}
		};
		JavaModule javaModule = new JavaModule() {
			@Override
			public Map<FunctionSignature, Function> getFunctions() {
				return Map.of(FunctionSignature.of("read", 0), constantFunction(1));
			}

			@Override
			public ModuleMeta getModuleMeta() {
				return customMetadata;
			}
		};
		HybridModule hybrid = new HybridModule("module {kind: \"hybrid\"}; def jq: 1;",
				Map.of(FunctionSignature.of("java", 0), constantFunction(2)));
		Environment<JsonNode> recent = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.registerModule("java", javaModule).registerModule("hybrid", hybrid).build();
		JsonNode javaMetadata = recent.compile("\"java\" | modulemeta").apply(NullNode.getInstance()).get(0);
		assertThat(javaMetadata.path("kind").asText()).isEqualTo("java");
		assertThat(javaMetadata.path("defs").get(0).asText()).isEqualTo("read/0");
		JsonNode hybridMetadata = recent.compile("\"hybrid\" | modulemeta").apply(NullNode.getInstance()).get(0);
		assertThat(hybridMetadata.path("kind").asText()).isEqualTo("hybrid");
		assertThat(hybridMetadata.path("defs").toString()).isEqualTo("[\"jq/0\",\"java/0\"]");
		assertThat(run(recent, "[\"java\", \"hybrid\"][] | modulemeta | .kind"))
				.containsExactly("\"java\"", "\"hybrid\"");

		Environment<JsonNode> old = builder().registerModule("java", javaModule).build();
		assertThat(old.compile("\"java\" | modulemeta").apply(NullNode.getInstance()).get(0).has("defs")).isFalse();
	}

	@Test
	public void testModuleMetaValidatesInputAndCanBeShadowed() {
		Environment<JsonNode> env = builder().build();
		assertThatThrownBy(() -> env.compile("1 | modulemeta").apply(NullNode.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("modulemeta input module name must be a string");
		assertThatThrownBy(() -> env.compile("\"missing\" | modulemeta").apply(NullNode.getInstance()))
				.isInstanceOf(ModuleNotFoundException.class);
		assertThat(run(env, "def modulemeta: 42; \"missing\" | modulemeta")).containsExactly("42");
	}
}
