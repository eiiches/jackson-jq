package examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class Usage {
	/**
	 * @see https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws IOException {
		// First of all, you have to prepare a Scope which s a container of built-in/user-defined functions and variables.
		Scope rootScope = Scope.newEmptyScope();

		// Scope#loadFunctions(ClassLoader) loads built-in functions (implemented in java) via ServiceLoader mechanism
		// and other built-in functions (implemented in jq) from classpath:net/thisptr/jackson/jq/jq.json.
		rootScope.loadFunctions(Scope.class.getClassLoader());

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {
			@Override
			public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
				final List<JsonNode> out = new ArrayList<>();
				for (final JsonNode times : args.get(0).apply(scope, in))
					out.add(new TextNode(Strings.repeat(in.asText(), times.asInt())));
				return out;
			}
		});

		// After this initial setup, rootScope should not be modified (via Scope#setValue(...),
		// Scope#addFunction(...), etc.) so that it can be shared (in a read-only manner) across mutliple threads
		// because you want to avoid heavy lifting of loading built-in functions every time which involves
		// file system operations and a lot of parsing.

		// Instead of modifying the rootScope directly, you can create a child Scope. This is especially useful when
		// you want to use variables or functions that is only local to the specific execution context (such as
		// a thread, request, etc).
		// Creating a child Scope is a very light-weight operation that just allocates a Scope and sets
		// one of its fields to point to the given parent scope. It's totally okay to create a child Scope
		// per every apply() invocations if you need to do so.
		Scope childScope = Scope.newChildScope(rootScope);

		// Scope#setValue(...) sets a custom variable that can be used from jq expressions. This variable is local to the
		// childScope and cannot be accessed from the rootScope. The rootScope will not be modified by this call.
		childScope.setValue("param", IntNode.valueOf(42));

		// JsonQuery#compile(...) parses and compiles a given expression. The resulting JsonQuery instance
		// is immutable and thread-safe. It should be reused as possible if you repeatedly use the same expression.
		JsonQuery q = JsonQuery.compile("$param * 2");

		// You need a JsonNode to use as an input to the JsonQuery. There are many ways you can grab a JsonNode.
		// In this example, we just parse a JSON text into a JsonNode.
		JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");

		// Finally, JsonQuery#apply(...) executes the query with given input and returns a list of JsonNode.
		// The childScope will not be modified by this call because it internally creates a child scope as necessary.
		final List<JsonNode> out = q.apply(childScope, in);

		System.out.println(out); // => [84]
	}
}
