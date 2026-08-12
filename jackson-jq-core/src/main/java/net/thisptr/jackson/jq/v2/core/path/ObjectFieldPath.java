package net.thisptr.jackson.jq.v2.core.path;

import java.util.Iterator;
import java.util.Map.Entry;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ObjectFieldPath<JsonNode> implements Path<JsonNode> {
	public final String key;
	private final Path<JsonNode> parent;

	public static <JsonNode> @Nullable ObjectFieldPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, String key) {
		if (parent == null)
			return null;
		return new ObjectFieldPath<>(parent, key);
	}

	public ObjectFieldPath(Path<JsonNode> parent, String key) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		this.key = key;
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, jsonProvider.createString(key));
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, key, permissive);
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, key, mutation, makeParent);
		}, makeParent);
	}

	private static <JsonNode> @Nullable JsonNode mutate(JsonProvider<JsonNode> jsonProvider, @Var @Nullable JsonNode in, String key, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		if (in == null || jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			if (!makeParent)
				return in;
			in = jsonProvider.createObject();
		}
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			JsonNode newobj = jsonProvider.createObject();
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				jsonProvider.set(newobj, entry.getKey(), entry.getValue());
			}
			JsonNode newval = mutation.apply(jsonProvider.get(newobj, key));
			if (newval != null)
				jsonProvider.set(newobj, key, newval);
			return newobj;
		} else {
			throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(jsonProvider, in), key));
		}
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, String key, boolean permissive) throws JsonQueryException {
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ObjectFieldPath.chainIfNotNull(ppath, key));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			JsonNode n = jsonProvider.get(pobj, key);
			output.emit(n == null ? jsonProvider.createNull() : n, ObjectFieldPath.chainIfNotNull(ppath, key));
		} else {
			if (!permissive)
				throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(jsonProvider, pobj), key));
		}
	}
}
