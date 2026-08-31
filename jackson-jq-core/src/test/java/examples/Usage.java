package examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.JsonQueryBindings;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class Usage {
	/**
	 * @see https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws IOException, URISyntaxException {
		// You need a JsonProvider which is an abstraction of a JSON library (Jackson 2, Jackson 3, Gson, etc.)
		Jackson2JsonProviderImpl jsonProvider = Jackson2JsonProviderImpl.getInstance();

		// First of all, prepare an Environment via EnvironmentBuilder, configured with JSON provider and JQ version.
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				// You can also define a custom function. E.g.
				.defineFunction(FunctionSignature.of("repeat", 1), new Function() {
					@Override
					public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, Version jqVersion) {
						return (frame, in, path, output) -> {
							args.get(0).apply(frame, in, UntrackedPath.getInstance(), (time, opath) -> {
								output.emit(jsonProvider.createString(Strings.repeat(jsonProvider.asString(in), jsonProvider.asInt(time))), UntrackedPath.getInstance());
							});
						};
					}
				})
				// ClassPathModuleLoader is used by default, so import statements already work out of the box.
				// Here we additionally chain in a FileSystemModuleLoader so imports can also resolve to
				// modules on disk (or classpath resources), not just ServiceLoader-registered modules.
				.setModuleLoader(new ChainedModuleLoader<>(
						ClassPathModuleLoader.getInstance(),
						new FileSystemModuleLoader<>(jsonProvider, Versions.JQ_1_7,
								FileSystems.getDefault().getPath("").toAbsolutePath(), // search modules in the actual file system
								Paths.get(Usage.class.getClassLoader().getResource("classpath_modules").toURI())) // or in the classpath resources
				))
				// declareVariable(...) declares a custom variable that can be used from jq expressions, with
				// no value -- a value must be supplied via JsonQueryBindings on every apply() call.
				.declareVariable("param")
				.build();

		// env.compile(...) parses, resolves symbols, and compiles a given expression.
		JsonQuery<JsonNode> q = env.compile("$param * 2");

		// You need a JsonNode to use as an input to the JsonQuery.
		JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");

		// A compiled query is reused with different variable and function bindings for each invocation.
		JsonQueryBindings<JsonNode> firstBindings = JsonQueryBindings.<JsonNode>builder()
				.setVariable("param", jsonProvider.createNumber(42))
				.build();
		q.apply(in, firstBindings, System.out::println); // => 84

		JsonQueryBindings<JsonNode> secondBindings = JsonQueryBindings.<JsonNode>builder()
				.setVariable("param", () -> jsonProvider.createNumber(7)) // suppliers are evaluated on each reference
				.build();
		q.apply(in, secondBindings, System.out::println); // => 14
	}
}
