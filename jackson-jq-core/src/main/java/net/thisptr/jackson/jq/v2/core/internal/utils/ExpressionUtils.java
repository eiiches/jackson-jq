package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.IdentifierKeyFieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringKeyFieldConstructionAst;
import net.thisptr.jackson.jq.v2.core.internal.ast.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class ExpressionUtils {

	/**
	 * @param jsonProvider the JSON provider
	 * @param expr the AST node to evaluate
	 * @return null if expr is not a constant
	 */
	@SuppressWarnings("unchecked")
	public static <JsonNode> @Nullable JsonNode evaluateLiteralExpression(JsonProvider<JsonNode> jsonProvider, AstNode expr) {
		if (expr instanceof ObjectConstruction) {
			JsonNode obj = jsonProvider.createObject();

			for (FieldConstructionAst field : ((ObjectConstruction) expr).fields) {
				if (field instanceof IdentifierKeyFieldConstructionAst) {
					IdentifierKeyFieldConstructionAst f = (IdentifierKeyFieldConstructionAst) field;
					String k = f.key;
					AstNode valueExpr = f.value;

					if (valueExpr == null) // this field depends on input and is not a constant
						return null;

					JsonNode v = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else if (field instanceof StringKeyFieldConstructionAst) {
					StringKeyFieldConstructionAst f = (StringKeyFieldConstructionAst) field;
					AstNode valueExpr = f.value;
					if (!(f.key instanceof StringLiteral)) // then the key is string interpolation and not a constant
						return null;
					if (valueExpr == null) // this field depends on input and is not a constant
						return null;
					String k = ((StringLiteral) f.key).value();

					JsonNode v = evaluateLiteralExpression(jsonProvider, valueExpr);
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

			AstNode tuple = ((ArrayConstruction) expr).q;
			if (tuple == null)
				return array; // empty

			if (tuple instanceof Tuple) {
				List<AstNode> values = ((Tuple) tuple).qs;
				for (AstNode valueExpr : values) {
					JsonNode value = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (value == null)
						return null;

					jsonProvider.add(array, value);
				}
			} else {
				JsonNode value = evaluateLiteralExpression(jsonProvider, tuple);
				if (value == null)
					return null;
				jsonProvider.add(array, value);
			}

			return array;
		} else if (expr instanceof ValueLiteral) {
			return ((ValueLiteral) expr).value(jsonProvider);
		} else {
			return null;
		}
	}

}
