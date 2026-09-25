package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

final class FileFunctionSupport {
	/**
	 * Every option member is optional, so each declared type admits its absence.
	 */
	static final Type OPTIONAL_STRING;
	static final Type OPTIONAL_BOOLEAN;

	static {
		OPTIONAL_STRING = UnionType.of(StringType.getInstance(), UndefinedType.getInstance());
		OPTIONAL_BOOLEAN = UnionType.of(BooleanType.getInstance(), UndefinedType.getInstance());
	}

	/**
	 * The members a write shares regardless of what it writes.
	 */
	static final Map<String, Type> COMMON_WRITE_OPTIONS = Map.of(
			"append", OPTIONAL_BOOLEAN,
			"create_parents", OPTIONAL_BOOLEAN,
			"mkdirs", OPTIONAL_BOOLEAN);

	private FileFunctionSupport() {
	}

	static <JsonNode> java.nio.file.Path parsePath(JsonProvider<JsonNode> jsonProvider, JsonNode node, String function) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " path must be a string, but got " + type);
		String value = jsonProvider.getString(node);
		try {
			return Paths.get(value);
		} catch (InvalidPathException | SecurityException e) {
			throw new JsonQueryException(function + " invalid path " + value + ": " + e.getMessage(), e);
		}
	}

	static <JsonNode> void checkAllowedOptionMembers(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function, java.util.Set<String> allowed) {
		JsonNodeType type = jsonProvider.getNodeType(options);
		if (type != JsonNodeType.OBJECT)
			throw new JsonQueryException(function + " options must be an object, but got " + type);
		Iterator<String> names = jsonProvider.getObjectMemberNames(options);
		while (names.hasNext()) {
			String name = names.next();
			if (!allowed.contains(name))
				throw new JsonQueryException(function + " options contains unknown member: " + name);
		}
	}

	static <JsonNode> Charset parseCharset(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function) {
		checkAllowedOptionMembers(jsonProvider, options, function, java.util.Collections.singleton("encoding"));
		return parseEncodingOption(jsonProvider, options, function);
	}

	static <JsonNode> Charset parseEncodingOption(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function) {
		if (!jsonProvider.hasObjectMember(options, "encoding"))
			return StandardCharsets.UTF_8;
		JsonNode encoding = jsonProvider.getObjectMemberOrThrow(options, "encoding");
		if (!jsonProvider.isString(encoding))
			throw new JsonQueryException(function + " encoding must be a string");
		String name = jsonProvider.getString(encoding);
		try {
			return Charset.forName(name);
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " unsupported encoding: " + name, e);
		}
	}

	static <JsonNode> boolean parseBooleanOption(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function, String name, boolean defaultValue) {
		if (!jsonProvider.hasObjectMember(options, name))
			return defaultValue;
		JsonNode value = jsonProvider.getObjectMemberOrThrow(options, name);
		if (!jsonProvider.isBoolean(value))
			throw new JsonQueryException(function + " " + name + " must be a boolean");
		return jsonProvider.getBoolean(value);
	}

	static <JsonNode> @Nullable String parseIndentOption(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function) {
		if (!jsonProvider.hasObjectMember(options, "indent"))
			return null;
		JsonNode value = jsonProvider.getObjectMemberOrThrow(options, "indent");
		JsonNodeType type = jsonProvider.getNodeType(value);
		if (type == JsonNodeType.NULL)
			return null;
		if (type == JsonNodeType.NUMBER) {
			Integer exact = jsonProvider.getNumberAsIntExact(value);
			if (exact == null || exact < 0)
				throw new JsonQueryException(function + " indent must be a non-negative integer, boolean, or string");
			return " ".repeat(exact);
		}
		if (type == JsonNodeType.BOOLEAN) {
			return jsonProvider.getBoolean(value) ? "  " : null;
		}
		if (type == JsonNodeType.STRING) {
			return jsonProvider.getString(value);
		}
		throw new JsonQueryException(function + " indent must be a non-negative integer, boolean, or string");
	}

	static <JsonNode> boolean parseCreateParentsOption(JsonProvider<JsonNode> jsonProvider, JsonNode options, String function) {
		boolean createParents = parseBooleanOption(jsonProvider, options, function, "create_parents", false);
		boolean mkdirs = parseBooleanOption(jsonProvider, options, function, "mkdirs", false);
		return createParents || mkdirs;
	}

	static void write(Path file, byte[] bytes, boolean append, boolean createParents, String function) {
		try {
			if (createParents) {
				Path parent = file.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
			}
			if (append) {
				Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
			} else {
				Files.write(file, bytes);
			}
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException(function + " failed for " + file + ": " + e.getMessage(), e);
		}
	}
}
