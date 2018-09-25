package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.MoreExceptions;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public abstract class FieldAccess implements Expression {
	protected final Expression target;
	protected final boolean permissive;

	public FieldAccess(final Expression target, final boolean permissive) {
		this.target = target;
		this.permissive = permissive;
	}

	protected static void emitAllPath(final boolean permissive, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to iterate through %s", pobj)); // TODO: truncate
		if (pobj.isNull()) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null (null)");
		} else if (pobj.isArray()) {
			for (int i = 0; i < pobj.size(); ++i)
				output.emit(pobj.get(i), ArrayIndexPath.chainIfNotNull(ppath, i));
		} else if (pobj.isObject()) {
			final Iterator<Entry<String, JsonNode>> iter = pobj.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				output.emit(entry.getValue(), ObjectFieldPath.chainIfNotNull(ppath, entry.getKey()));
			}
		} else {
			if (!permissive)
				throw new JsonQueryException(MoreExceptions.format("Cannot iterate over %s", pobj));
		}
	}

	protected static void emitObjectFieldPath(boolean permissive, String key, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", TextNode.valueOf(key), pobj)); // TODO: truncate
		final Optional<JsonNode> obj = ObjectFieldPath.resolve(pobj, key, permissive);
		if (obj.isPresent()) {
			output.emit(obj.get(), ObjectFieldPath.chainIfNotNull(ppath, key));
		}
	}

	protected static void emitArrayIndexPath(boolean permissive, long _index, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", _index, pobj));
		final Optional<JsonNode> obj = ArrayIndexPath.resolve(pobj, (int) _index, permissive);
		if (obj.isPresent()) {
			final Path path = ArrayIndexPath.chainIfNotNull(ppath, (int) _index);
			output.emit(obj.get(), path);
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper(); // FIXME

	private static String truncateWithDot(final String text, final int len) { // FIXME: move to somewhere else
		if (text.length() <= len)
			return text;
		return text.substring(0, len - 3) + "...";
	}

	protected static void emitArrayRangeIndexPath(boolean permissive, final Long start, final Long end, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null) {
			final ObjectNode subpath = MAPPER.createObjectNode();
			subpath.set("start", start == null ? NullNode.getInstance() : LongNode.valueOf(start));
			subpath.set("end", end == null ? NullNode.getInstance() : LongNode.valueOf(end));
			throw new JsonQueryException(String.format("Invalid path expression near attempt to access element %s of %s", truncateWithDot(subpath.toString(), 14), pobj));
		}
		final Optional<JsonNode> obj = ArrayRangeIndexPath.resolve(pobj, start, end, permissive);
		if (obj.isPresent()) {
			final Path path = ArrayRangeIndexPath.chainIfNotNull(ppath, start, end);
			output.emit(obj.get(), path);
		}
	}
}
