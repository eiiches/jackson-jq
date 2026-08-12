package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ExpressionUtils {

	/**
	 * @param jsonProvider the JSON provider
	 * @param expr the expression to evaluate
	 * @return null if expr is not a constant
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> JsonNode evaluateLiteralExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> expr) {
		if (expr instanceof ObjectConstruction) {
			JsonNode obj = jsonProvider.createObject();

			for (FieldConstruction<JsonNode> field : ((ObjectConstruction<JsonNode>) expr).fields) {
				if (field instanceof IdentifierKeyFieldConstruction) {
					IdentifierKeyFieldConstruction<JsonNode> f = (IdentifierKeyFieldConstruction<JsonNode>) field;
					String k = f.key;

					if (f.value == null) // this field depends on input and is not a constant
						return null;

					JsonNode v = evaluateLiteralExpression(jsonProvider, f.value);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else if (field instanceof StringKeyFieldConstruction) {
					StringKeyFieldConstruction<JsonNode> f = (StringKeyFieldConstruction<JsonNode>) field;
					if (!(f.key instanceof StringLiteral)) // then the key is string interpolation and not a constant
						return null;
					String k = ((StringLiteral<JsonNode>) f.key).value();

					JsonNode v = evaluateLiteralExpression(jsonProvider, f.value);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else {
					return null;
				}
			}

			return obj;
		} else if (expr instanceof ArrayConstruction) {
			JsonNode array = jsonProvider.createArray();

			Expression<JsonNode> tuple = ((ArrayConstruction<JsonNode>) expr).q;
			if (tuple == null)
				return array; // empty

			if (tuple instanceof Tuple) {
				List<Expression<JsonNode>> values = ((Tuple<JsonNode>) tuple).qs;
				for (Expression<JsonNode> valueExpr : values) {
					JsonNode value = evaluateLiteralExpression(jsonProvider, valueExpr);
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
