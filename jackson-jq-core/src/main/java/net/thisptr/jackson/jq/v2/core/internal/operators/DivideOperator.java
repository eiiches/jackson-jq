package net.thisptr.jackson.jq.v2.core.internal.operators;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class DivideOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private final @Nullable Version version;

	public DivideOperator() {
		this(null);
	}

	public DivideOperator(@Nullable Version version) {
		this.version = version;
	}

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		JsonNodeType ltype = jsonProvider.getNodeType(lhs);
		JsonNodeType rtype = jsonProvider.getNodeType(rhs);
		if (ltype == JsonNodeType.NUMBER && rtype == JsonNodeType.NUMBER) {
			double divisor = jsonProvider.getNumberAsDoubleRounded(rhs);
			double dividend = jsonProvider.getNumberAsDoubleRounded(lhs);
			if (divisor == 0.0)
				throw new JsonQueryException(ExceptionMessages.format(jsonProvider, version, "%s and %s cannot be divided because the divisor is zero", lhs, rhs));
			return JsonNodeUtils.asNumericNode(jsonProvider, dividend / divisor);
		} else if (ltype == JsonNodeType.STRING && rtype == JsonNodeType.STRING) {
			List<JsonNode> result = new ArrayList<>();
			for (String token : Strings.split(jsonProvider.getString(lhs), jsonProvider.getString(rhs)))
				result.add(jsonProvider.createString(token));
			return jsonProvider.createArray(result);
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot be divided", lhs, rhs);
		}
	}

	@Override
	public String image() {
		return "/";
	}
}
