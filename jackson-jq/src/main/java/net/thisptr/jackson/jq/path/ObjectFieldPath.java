package net.thisptr.jackson.jq.path;

import java.util.Iterator;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ObjectFieldPath<JsonNode> implements Path<JsonNode> {
	public final String key;
	private final Path<JsonNode> parent;

	public static <JsonNode> ObjectFieldPath<JsonNode> chainIfNotNull(final Path<JsonNode> parent, final String key) {
		if (parent == null)
			return null;
		return new ObjectFieldPath<>(parent, key);
	}

	public ObjectFieldPath(final Path<JsonNode> parent, final String key) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		this.key = key;
	}

	@Override
	public void toJsonNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, jsonProvider.createString(key));
	}

	@Override
	public void get(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, key, permissive);
		}, permissive);
	}

	@Override
	public JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			return mutate(jsonProvider, oldval, key, mutation, makeParent);
		}, makeParent);
	}

	private static <JsonNode> JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, JsonNode in, final String key, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		if (in == null || jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			if (!makeParent)
				return in;
			in = jsonProvider.createObject();
		}
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			final JsonNode newobj = jsonProvider.createObject();
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				jsonProvider.set(newobj, entry.getKey(), entry.getValue());
			}
			final JsonNode newval = mutation.apply(jsonProvider.get(newobj, key));
			if (newval != null)
				jsonProvider.set(newobj, key, newval);
			return newobj;
		} else {
			throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(jsonProvider, in), key));
		}
	}

	public static <JsonNode> void resolve(final JsonProvider<JsonNode> jsonProvider, JsonNode pobj, Path<JsonNode> ppath, PathOutput<JsonNode> output, String key, boolean permissive) throws JsonQueryException {
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), ObjectFieldPath.chainIfNotNull(ppath, key));
		} else if (jsonProvider.getNodeType(pobj) == JsonNodeType.OBJECT) {
			final JsonNode n = jsonProvider.get(pobj, key);
			output.emit(n == null ? jsonProvider.createNull() : n, ObjectFieldPath.chainIfNotNull(ppath, key));
		} else {
			if (!permissive)
				throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(jsonProvider, pobj), key));
		}
	}
}
