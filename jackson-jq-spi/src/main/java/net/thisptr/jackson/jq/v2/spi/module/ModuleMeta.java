package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Metadata associated with a jq module, including module directives,
 * declared dependencies (imports and includes), and function definitions.
 */
public interface ModuleMeta {

	/**
	 * Returns the module metadata defined in the {@code module { ... };} directive.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the {@link JsonProvider} used to create JSON node values
	 * @return a map of metadata keys to JSON node values, or an empty map if not present
	 */
	default <JsonNode> Map<String, JsonNode> getMetadata(JsonProvider<JsonNode> jsonProvider) {
		return Collections.emptyMap();
	}

	/**
	 * Returns the list of dependencies (imports and includes) declared by this module.
	 *
	 * @return the list of dependencies, or an empty list if none
	 */
	default List<Dependency> getDependencies() {
		return Collections.emptyList();
	}

	/**
	 * Represents a module dependency declared via an {@code import} or {@code include} statement.
	 */
	interface Dependency {

		/**
		 * Returns the relative path of the dependency (e.g. {@code "foo/bar"}).
		 *
		 * @return the relative path
		 */
		String getRelpath();

		/**
		 * Returns whether this is a data import (e.g. {@code import "data" as $data;}).
		 *
		 * @return {@code true} if this is a data import; {@code false} otherwise
		 */
		boolean isData();

		/**
		 * Returns the alias assigned to the imported module or variable (e.g. {@code "bar"}
		 * in {@code import "foo" as bar;} or {@code "data"} in {@code import "data" as $data;}).
		 * For {@code include} statements without an {@code as} clause, this returns {@code null}.
		 *
		 * @return the alias name, or {@code null} if no alias was specified
		 */
		@Nullable String getAlias();

		/**
		 * Returns the metadata associated with the import statement (e.g. {@code import "foo" as bar { "search": "..." };}).
		 *
		 * @param <JsonNode> the JSON node type
		 * @param jsonProvider the {@link JsonProvider} used to create JSON node values
		 * @return a map of import metadata keys to JSON node values, or an empty map if none
		 */
		default <JsonNode> Map<String, JsonNode> getImportMetadata(JsonProvider<JsonNode> jsonProvider) {
			return Collections.emptyMap();
		}
	}

	/**
	 * Returns the list of function signatures defined/exported by this module.
	 *
	 * @return the list of function signatures
	 */
	List<FunctionSignature> getDefinitions();
}
