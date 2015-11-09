package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonArgumentException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils.Mutation;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.FieldAccess.ResolvedPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("del/1")
public class DelFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("del", in, JsonNodeType.OBJECT, JsonNodeType.ARRAY, JsonNodeType.NULL);

		final JsonQuery arg = args.get(0);
		if (!(arg instanceof FieldAccess))
			throw new IllegalJsonArgumentException("1st argument to del() must be a field access");

		final ResolvedPath resolvedPath = ((FieldAccess) arg).resolvePath(scope, in);
		if (!(resolvedPath.target instanceof ThisObject))
			throw new IllegalJsonArgumentException("cannot delete from " + resolvedPath.target);

		return Collections.singletonList(JsonNodeUtils.mutate(scope.getObjectMapper(), in, resolvedPath.path, new Mutation() {
			@Override
			public JsonNode apply(JsonNode value) {
				return null;
			}
		}, false));
	}
}
