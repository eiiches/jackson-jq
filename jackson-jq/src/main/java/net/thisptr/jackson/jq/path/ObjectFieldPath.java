package net.thisptr.jackson.jq.path;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ObjectFieldPath implements Path {
	public final String key;
	private final Path parent;

	public static ObjectFieldPath chainIfNotNull(final Path parent, final String key) {
		if (parent == null)
			return null;
		return new ObjectFieldPath(parent, key);
	}

	public ObjectFieldPath(final Path parent, final String key) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		this.key = key;
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		parent.toJsonNode(out);
		out.add(new TextNode(key));
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			resolve(parent, ppath, output, key, permissive);
		}, permissive);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			return mutate(oldval, key, mutation, makeParent);
		}, makeParent);
	}

	private static JsonNode mutate(JsonNode in, final String key, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		if (in == null || in.isNull()) {
			if (!makeParent)
				return in;
			in = MAPPER.createObjectNode();
		}
		if (in.isObject()) {
			final ObjectNode newobj = MAPPER.createObjectNode();
			final Iterator<Entry<String, JsonNode>> iter = in.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				newobj.set(entry.getKey(), entry.getValue());
			}
			final JsonNode newval = mutation.apply(newobj.get(key));
			if (newval != null)
				newobj.set(key, newval);
			return newobj;
		} else {
			throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(in), key));
		}
	}

	public static void resolve(JsonNode pobj, Path ppath, PathOutput output, String key, boolean permissive) throws JsonQueryException {
		if (pobj.isNull()) {
			output.emit(NullNode.getInstance(), ObjectFieldPath.chainIfNotNull(ppath, key));
		} else if (pobj.isObject()) {
			final JsonNode n = pobj.get(key);
			output.emit(n == null ? NullNode.getInstance() : n, ObjectFieldPath.chainIfNotNull(ppath, key));
		} else {
			if (!permissive)
				throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(pobj), key));
		}
	}
}
