package examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class Usage {
	/**
	 * @see https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, URISyntaxException {
		// You need a JsonProvider which is an abstraction of a JSON library (Jackson 2, Jackson 3, Gson, etc.)
		Jackson2JsonProviderImpl jsonProvider = Jackson2JsonProviderImpl.getInstance();

		// First of all, prepare an Environment container configured with JSON provider and JQ version.
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_6);

		// You can also define a custom function using FunctionFactory. E.g.
		env.addFunctionFactory(FunctionNameAndArity.of("repeat", 1), new FunctionFactory() {
			@Override
			public <N> Function<N> createFunction(JsonProvider<N> fprovider, List<Expression> fargs, Version ver) {
				return (scope, in, path, output) -> {
					fargs.get(0).apply(scope, in, (time) -> {
						output.emit(fprovider.createString(Strings.repeat(fprovider.asText(in), fprovider.asInt(time))), null);
					});
				};
			}
		});

		// For import statements to work, set ModuleLoader.
		env.setModuleLoader(new ChainedModuleLoader<>(new ModuleLoader[] {
				ClassPathModuleLoader.getInstance(),
				new FileSystemModuleLoader<>(Scope.newEmptyScope(jsonProvider), Versions.JQ_1_6,
						FileSystems.getDefault().getPath("").toAbsolutePath(), // search modules in the actual file system
						Paths.get(Environment.class.getClassLoader().getResource("classpath_modules").toURI())), // or in the classpath resources
		}));

		// addVariable(...) sets a custom variable that can be used from jq expressions.
		env.addVariable("param", jsonProvider.createNumber(42));

		// env.compile(...) parses, resolves symbols, and compiles a given expression.
		JsonQuery<JsonNode> q = env.compile("$param * 2");

		// You need a JsonNode to use as an input to the JsonQuery.
		JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");

		// Finally, JsonQuery#apply(...) executes the query with given input and produces 0, 1 or more JsonNode.
		List<JsonNode> out = new ArrayList<>();
		q.apply(in, (outNode, path) -> out.add(outNode));
		System.out.println(out); // => [84]
	}
}
