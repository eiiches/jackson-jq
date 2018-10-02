package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;
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
			throw new JsonQueryException("Invalid path expression near attempt to iterate through %s", JsonNodeUtils.toString(pobj));
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
				throw new JsonQueryTypeException("Cannot iterate over %s", pobj);
		}
	}

	protected static void emitObjectFieldPath(boolean permissive, String key, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", JsonNodeUtils.toString(TextNode.valueOf(key)), JsonNodeUtils.toString(pobj));
		ObjectFieldPath.resolve(pobj, ppath, output, key, permissive);
	}

	protected static void emitArrayIndexPath(boolean permissive, long _index, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null)
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", _index, JsonNodeUtils.toString(pobj));
		ArrayIndexPath.resolve(pobj, ppath, output, (int) _index, permissive);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper(); // FIXME

	protected static void emitArrayRangeIndexPath(boolean permissive, final Long start, final Long end, final JsonNode pobj, final Path ppath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		if (requirePath && ppath == null) {
			final ObjectNode subpath = MAPPER.createObjectNode();
			subpath.set("start", start == null ? NullNode.getInstance() : LongNode.valueOf(start));
			subpath.set("end", end == null ? NullNode.getInstance() : LongNode.valueOf(end));
			throw new JsonQueryException("Invalid path expression near attempt to access element %s of %s", Strings.truncate(JsonNodeUtils.toString(subpath), 14), JsonNodeUtils.toString(pobj));
		}
		ArrayRangeIndexPath.resolve(pobj, ppath, output, start, end, permissive);
	}
}
