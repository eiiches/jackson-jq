package examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.module.loaders.BuiltinModuleLoader;
import net.thisptr.jackson.jq.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.path.Path;

import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;

public class Usage {
	/**
	 * @see https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, URISyntaxException {
		// You need a JsonProvider which is an abstraction of a JSON library (Jackson 2, Jackson 3, Gson, etc.)
		Jackson2JsonProviderImpl jsonProvider = Jackson2JsonProviderImpl.getInstance();

		// First of all, you have to prepare a Scope which is a container of built-in/user-defined functions and variables.
		Scope<JsonNode> rootScope = Scope.newEmptyScope(jsonProvider);

		// Use BuiltinFunctionLoader to load built-in functions from the classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function<JsonNode>() {
			@Override
			public void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
				args.get(0).apply(scope, in, (time) -> {
					output.emit(scope.jsonProvider().createString(Strings.repeat(scope.jsonProvider().asText(in), scope.jsonProvider().asInt(time))), null);
				});
			}
		});

		// For import statements to work, you need to set ModuleLoader. BuiltinModuleLoader uses ServiceLoader mechanism to
		// load Module implementations.
		rootScope.setModuleLoader(BuiltinModuleLoader.getInstance());

		// Alternatively, you can also use/combine FileSystemModuleLoader to load .jq/.json files from the file system.
		rootScope.setModuleLoader(new ChainedModuleLoader<>(new ModuleLoader[] {
				BuiltinModuleLoader.getInstance(),
				new FileSystemModuleLoader(rootScope, Versions.JQ_1_6,
						FileSystems.getDefault().getPath("").toAbsolutePath(), // search modules in the actual file system
						Paths.get(Scope.class.getClassLoader().getResource("classpath_modules").toURI())), // or in the classpath resources
		}));

		// After this initial setup, rootScope should not be modified (via Scope#setValue(...),
		// Scope#addFunction(...), etc.) so that it can be shared (in a read-only manner) across multiple threads
		// because you want to avoid heavy lifting of loading built-in functions every time which involves
		// file system operations and a lot of parsing.

		// Instead of modifying the rootScope directly, you can create a child Scope. This is especially useful when
		// you want to use variables or functions that is only local to the specific execution context (such as
		// a thread, request, etc).
		// Creating a child Scope is a very light-weight operation that just allocates a Scope and sets
		// one of its fields to point to the given parent scope. It's totally okay to create a child Scope
		// per every apply() invocations if you need to do so.
		Scope<JsonNode> childScope = Scope.newChildScope(rootScope);

		// Scope#setValue(...) sets a custom variable that can be used from jq expressions. This variable is local to the
		// childScope and cannot be accessed from the rootScope. The rootScope will not be modified by this call.
		childScope.setValue("param", jsonProvider.createInt(42));

		// JsonQuery#compile(...) parses and compiles a given expression. The resulting JsonQuery instance
		// is immutable and thread-safe. It should be reused as possible if you repeatedly use the same expression.
		JsonQuery<JsonNode> q = JsonQuery.compile("$param * 2", Versions.JQ_1_6);

		// You need a JsonNode to use as an input to the JsonQuery. There are many ways you can grab a JsonNode.
		// In this example, we just parse a JSON text into a JsonNode.
		JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");

		// Finally, JsonQuery#apply(...) executes the query with given input and produces 0, 1 or more JsonNode.
		// The childScope will not be modified by this call because it internally creates a child scope as necessary.
		final List<JsonNode> out = new ArrayList<>();
		q.apply(childScope, in, out::add);
		System.out.println(out); // => [84]
	}
}
