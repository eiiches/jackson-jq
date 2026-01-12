package net.thisptr.jackson.jq.internal.utils;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.internal.tree.Tuple;
import net.thisptr.jackson.jq.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.internal.tree.literal.ValueLiteral;

public class ExpressionUtils {

	/**
	 * @param jsonProvider the JSON provider
	 * @param expr the expression to evaluate
	 * @return null if expr is not a constant
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> JsonNode evaluateLiteralExpression(final JsonProvider<JsonNode> jsonProvider, final Expression<JsonNode> expr) {
		if (expr instanceof ObjectConstruction) {
			final JsonNode obj = jsonProvider.createObject();

			for (final FieldConstruction<JsonNode> field : ((ObjectConstruction<JsonNode>) expr).fields) {
				if (field instanceof IdentifierKeyFieldConstruction) {
					final IdentifierKeyFieldConstruction<JsonNode> f = (IdentifierKeyFieldConstruction<JsonNode>) field;
					final String k = f.key;

					if (f.value == null) // this field depends on input and is not a constant
						return null;

					final JsonNode v = evaluateLiteralExpression(jsonProvider, f.value);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else if (field instanceof StringKeyFieldConstruction) {
					final StringKeyFieldConstruction<JsonNode> f = (StringKeyFieldConstruction<JsonNode>) field;
					if (!(f.key instanceof StringLiteral)) // then the key is string interpolation and not a constant
						return null;
					final String k = ((StringLiteral<JsonNode>) f.key).value();

					final JsonNode v = evaluateLiteralExpression(jsonProvider, f.value);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else {
					return null;
				}
			}

			return obj;
		} else if (expr instanceof ArrayConstruction) {
			final JsonNode array = jsonProvider.createArray();

			final Expression<JsonNode> tuple = ((ArrayConstruction<JsonNode>) expr).q;
			if (tuple == null)
				return array; // empty

			if (tuple instanceof Tuple) {
				final List<Expression<JsonNode>> values = ((Tuple<JsonNode>) tuple).qs;
				for (final Expression<JsonNode> valueExpr : values) {
					final JsonNode value = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (value == null)
						return null;

					jsonProvider.add(array, value);
				}
			} else {
				jsonProvider.add(array, evaluateLiteralExpression(jsonProvider, tuple));
			}

			return array;
		} else if (expr instanceof ValueLiteral) {
			return ((ValueLiteral<JsonNode>) expr).value(jsonProvider);
		} else {
			return null;
		}
	}

}
