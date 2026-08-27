package net.thisptr.jackson.jq.v2.core.path;

import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ObjectFieldPath<JsonNode> implements Path<JsonNode> {
	public final String key;
	private final Path<JsonNode> parent;
	private final @Nullable Version version;

	public static <JsonNode> @Nullable ObjectFieldPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, String key) {
		return chainIfNotNull(parent, key, null);
	}

	public static <JsonNode> @Nullable ObjectFieldPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, String key, @Nullable Version version) {
		if (parent == null)
			return null;
		return new ObjectFieldPath<>(parent, key, version);
	}

	public ObjectFieldPath(Path<JsonNode> parent, String key) {
		this(parent, key, null);
	}

	public ObjectFieldPath(Path<JsonNode> parent, String key, @Nullable Version version) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		this.key = key;
		this.version = version;
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, jsonProvider.createString(key));
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, key, permissive, version);
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, key, mutation, makeParent, version);
		}, makeParent);
	}

	private static <JsonNode> @Nullable JsonNode mutate(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, String key, Mutation<JsonNode> mutation, boolean makeParent, @Nullable Version version) throws JsonQueryException {
		if (in == null || jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			if (!makeParent)
				return in;
			in = jsonProvider.createObject();
		}
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			JsonNode newobj = jsonProvider.createObject();
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				jsonProvider.set(newobj, entry.getKey(), entry.getValue());
			}
			JsonNode newval = mutation.apply(jsonProvider.get(newobj, key));
			if (newval != null)
				jsonProvider.set(newobj, key, newval);
			return newobj;
		} else {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createString(key)));
		}
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, String key, boolean permissive) throws JsonQueryException {
		resolve(jsonProvider, pobj, ppath, output, key, permissive, null);
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, Output<JsonNode> output, String key, boolean permissive, @Nullable Version version) throws JsonQueryException {
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ObjectFieldPath.chainIfNotNull(ppath, key, version));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			JsonNode n = jsonProvider.get(pobj, key);
			output.emit(n == null ? jsonProvider.createNull() : n, ObjectFieldPath.chainIfNotNull(ppath, key, version));
		} else {
			if (!permissive)
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, pobj, jsonProvider.createString(key)));
		}
	}
}
