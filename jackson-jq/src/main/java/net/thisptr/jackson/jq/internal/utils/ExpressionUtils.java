package net.thisptr.jackson.jq.internal.utils;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.internal.tree.Tuple;
import net.thisptr.jackson.jq.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.internal.tree.literal.ValueLiteral;

public class ExpressionUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * @param expr
	 * @return null if expr is not a constant
	 */
	public static JsonNode evaluateLiteralExpression(final Expression expr) {
		if (expr instanceof ObjectConstruction) {
			final ObjectNode obj = MAPPER.createObjectNode();

			for (final FieldConstruction field : ((ObjectConstruction) expr).fields) {
				if (field instanceof IdentifierKeyFieldConstruction) {
					final IdentifierKeyFieldConstruction f = (IdentifierKeyFieldConstruction) field;
					final String k = f.key;

					if (f.value == null) // this field depends on input and is not a constant
						return null;

					final JsonNode v = evaluateLiteralExpression(f.value);
					if (v == null)
						return null;

					obj.set(k, v);
				} else if (field instanceof StringKeyFieldConstruction) {
					final StringKeyFieldConstruction f = (StringKeyFieldConstruction) field;
					if (!(f.key instanceof StringLiteral)) // then the key is string interpolation and not a constant
						return null;
					final String k = ((StringLiteral) f.key).value().asText();

					final JsonNode v = evaluateLiteralExpression(f.value);
					if (v == null)
						return null;

					obj.set(k, v);
				} else {
					return null;
				}
			}

			return obj;
		} else if (expr instanceof ArrayConstruction) {
			final ArrayNode array = MAPPER.createArrayNode();

			final Expression tuple = ((ArrayConstruction) expr).q;
			if (tuple == null)
				return array; // empty

			if (tuple instanceof Tuple) {
				final List<Expression> values = ((Tuple) tuple).qs;
				for (final Expression valueExpr : values) {
					final JsonNode value = evaluateLiteralExpression(valueExpr);
					if (value == null)
						return null;

					array.add(value);
				}
			} else {
				array.add(evaluateLiteralExpression(tuple));
			}

			return array;
		} else if (expr instanceof ValueLiteral) {
			return ((ValueLiteral) expr).value();
		} else {
			return null;
		}
	}

}
