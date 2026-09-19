package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

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
		private final Map<String, JsonNode> datas = new HashMap<>();
		private final List<String> requested = new ArrayList<>();

		SourceLoader(String root) {
			this.root = root;
		}

		SourceLoader put(String path, String source) {
			sources.put(root + "/" + path, source);
			return this;
		}

		SourceLoader putData(String path, JsonNode data) {
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
			return dataAt(root + "/" + path, path);
		}

		SourceModule moduleAt(String name, String path) {
			String source = sources.get(name);
			if (source == null)
				throw new ModuleNotFoundException(path);
			return new SourceModule(this, name, source);
		}

		JsonNode dataAt(String name, String path) {
			JsonNode data = datas.get(name);
			if (data == null)
				throw new ModuleNotFoundException(path);
			return data;
		}
	}

	private static final class SourceModule implements JqModule<JsonNode> {
		private final SourceLoader owner;
		private final String name;
		private final String source;

		SourceModule(SourceLoader owner, String name, String source) {
			this.owner = owner;
			this.name = name;
			this.source = source;
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
		public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
			return owner.moduleAt(resolve(importPath, searchPath), importPath);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPath) {
			return owner.dataAt(resolve(importPath, searchPath), importPath);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			return o instanceof SourceModule && name.equals(((SourceModule) o).name);
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

	private static final class HybridModule implements JavaModule, JqModule<JsonNode> {
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
		public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
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
		return EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders();
	}

	/**
	 * Compared as text: which node class the compiler produced is not what these tests are about.
	 */
	private static List<String> run(Environment<JsonNode> env, String query) throws Exception {
		JsonQuery<JsonNode> expr = env.compile(query);
		List<String> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), value -> actual.add(value.toString()));
		return actual;
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
	public void testMissedLoaderFallsThroughToNextLoader() throws Exception {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new MissingModuleLoader())
				.addModuleLoader(new SourceLoader("/second").put("foo", "def one: 1;"))
				.build();

		assertThat(run(env, "import \"foo\" as foo; foo::one")).containsExactly("1");
	}

	@Test
	public void testFailingLoaderAbortsTheSearch() throws Exception {
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
	public void testAllLoadersMissing() throws Exception {
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
	public void testNonRelativeImportInsideModuleWalksEveryLoader() throws Exception {
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
	public void testRelativeImportIsResolvedByTheModuleItself() throws Exception {
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
	public void testRelativeDataImportIsResolvedByTheModuleItself() throws Exception {
		MissingModuleLoader other = new MissingModuleLoader();
		Environment<JsonNode> env = builder()
				.addModuleLoader(other)
				.addModuleLoader(new SourceLoader("/first")
						.put("lib/a", "import \"nums\" as $nums {search: \"./\"}; def one: $nums::nums;")
						.putData("lib/nums", IntNode.valueOf(7)))
				.build();

		assertThat(run(env, "import \"lib/a\" as a; a::one")).containsExactly("7");
		assertThat(other.requested).containsExactly("lib/a");
	}

	/**
	 * An environment can be handed either kind of module. jq source is compiled the first time a
	 * query calls into it, and not at all if no query does.
	 */
	@Test
	public void testImportedJqModuleFromEnvironmentIsCompiledOnUse() throws Exception {
		SourceLoader loader = new SourceLoader("/first").put("helper", "def three: 3;");
		SourceModule module = loader.moduleAt("/first/helper", "helper");

		Environment<JsonNode> env = builder()
				.addImportedModule("m", module)
				.build();

		assertThat(run(env, "m::three")).containsExactly("3");
		// Nothing referenced it, so nothing compiled it.
		assertThat(run(env, "1 + 1")).containsExactly("2");
	}

	@Test
	public void testHybridModuleCombinesJavaAndJqFunctions() throws Exception {
		FunctionSignature javaHelper = FunctionSignature.of("java_helper", 0);
		HybridModule module = new HybridModule(
				"module {\"kind\": \"hybrid\"}; def from_jq: java_helper;",
				Collections.singletonMap(javaHelper, constantFunction(7)));
		Environment<JsonNode> loaderEnvironment = builder()
				.addModuleLoader(new SingleModuleLoader(module))
				.build();
		Environment<JsonNode> importedEnvironment = builder()
				.addImportedModule("hybrid", module)
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
	public void testHybridModuleAllowsExactAndVariadicSignaturesToCoexist() throws Exception {
		HybridModule module = new HybridModule(
				"def shared: 1;",
				Collections.singletonMap(FunctionSignature.ofVariadic("shared"), constantFunction(2)));
		Environment<JsonNode> env = builder()
				.addImportedModule("hybrid", module)
				.build();

		assertThat(run(env, "[hybrid::shared, hybrid::shared(0)]")).containsExactly("[1,2]");
	}

	@Test
	public void testIncludeExposesJavaModuleFunctionsWithoutAQualifier() throws Exception {
		Map<FunctionSignature, Function> functions = new HashMap<>();
		functions.put(FunctionSignature.of("selected", 0), constantFunction(1));
		functions.put(FunctionSignature.ofVariadic("selected"), constantFunction(2));
		JavaModule module = () -> Collections.unmodifiableMap(functions);
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SingleModuleLoader(module))
				.build();

		assertThat(run(env, "include \"helpers\"; [selected, selected(0)]")).containsExactly("[1,2]");
	}

	@Test
	public void testLaterIncludeShadowsEarlierInclude() throws Exception {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first")
						.put("one", "def selected: 1;")
						.put("two", "def selected: 2;"))
				.build();

		assertThat(run(env, "include \"one\"; include \"two\"; selected")).containsExactly("2");
	}

	@Test
	public void testLocalDefinitionShadowsIncludeWhichShadowsEnvironment() throws Exception {
		FunctionSignature declared = FunctionSignature.of("declared", 0);
		FunctionSignature defined = FunctionSignature.of("defined", 0);
		Map<FunctionSignature, Function> functions = new HashMap<>();
		functions.put(declared, constantFunction(2));
		functions.put(defined, constantFunction(4));
		functions.put(FunctionSignature.of("length", 0), constantFunction(6));
		JavaModule module = () -> Collections.unmodifiableMap(functions);
		Environment<JsonNode> env = builder()
				.declareFunction(declared)
				.defineFunction(defined, constantFunction(3))
				.addModuleLoader(new SingleModuleLoader(module))
				.build();

		assertThat(run(env, "include \"helpers\"; [declared, defined, length]")).containsExactly("[2,4,6]");
		assertThat(run(env, "include \"helpers\"; def defined: 5; defined")).containsExactly("5");
	}

	@Test
	public void testRelativeImportFromTopLevelIsRejected() throws Exception {
		Environment<JsonNode> env = builder()
				.addModuleLoader(new SourceLoader("/first").put("b", "def two: 2;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"b\" as b {search: \"./\"}; b::two"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("can only be overriden from imported modules");
	}

	@Test
	public void testAbsoluteImportPathIsRejected() throws Exception {
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
	public void testCycleAcrossTwoLoadersIsReported() throws Exception {
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
	public void testSameModuleImportedTwiceIsCompiledOnce() throws Exception {
		SourceLoader loader = new SourceLoader("/first")
				.put("a", "import \"c\" as c; def one: c::three;")
				.put("b", "import \"c\" as c; def two: c::three;")
				.put("c", "def three: 3;");
		Environment<JsonNode> env = builder().addModuleLoader(loader).build();

		assertThat(run(env, "import \"a\" as a; import \"b\" as b; [a::one, b::two]")).containsExactly("[3,3]");
		assertThat(loader.requested).containsExactly("a", "c", "b", "c");
		assertThat(env.compile("import \"c\" as c; c::three")).isNotNull();
	}
}
