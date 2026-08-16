package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.ValueLiteralAstNode;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class ExpressionUtils {

	/**
	 * @param jsonProvider the JSON provider
	 * @param expr the AST node to evaluate
	 * @return null if expr is not a constant
	 */
	public static <JsonNode> @Nullable JsonNode evaluateLiteralExpression(JsonProvider<JsonNode> jsonProvider, AstNode expr) {
		if (expr instanceof ObjectConstructionAstNode) {
			JsonNode obj = jsonProvider.createObject();

			for (ObjectConstructionAstNode.FieldConstructionAst field : ((ObjectConstructionAstNode) expr).fields) {
				if (field instanceof ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) {
					ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst f = (ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst) field;
					String k = f.key;
					AstNode valueExpr = f.value;

					if (valueExpr == null) // this field depends on input and is not a constant
						return null;

					JsonNode v = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else if (field instanceof ObjectConstructionAstNode.StringKeyFieldConstructionAst) {
					ObjectConstructionAstNode.StringKeyFieldConstructionAst f = (ObjectConstructionAstNode.StringKeyFieldConstructionAst) field;
					AstNode valueExpr = f.value;
					if (!(f.key instanceof StringLiteralAstNode)) // then the key is string interpolation and not a constant
						return null;
					if (valueExpr == null) // this field depends on input and is not a constant
						return null;
					String k = ((StringLiteralAstNode) f.key).value();

					JsonNode v = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (v == null)
						return null;

					jsonProvider.set(obj, k, v);
				} else {
					return null;
				}
			}

			return obj;
		} else if (expr instanceof ArrayConstructionAstNode) {
			JsonNode array = jsonProvider.createArray();

			AstNode tuple = ((ArrayConstructionAstNode) expr).q;
			if (tuple == null)
				return array; // empty

			if (tuple instanceof TupleAstNode) {
				List<AstNode> values = ((TupleAstNode) tuple).qs;
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
		} else if (expr instanceof ValueLiteralAstNode) {
			return ((ValueLiteralAstNode) expr).value(jsonProvider);
		} else {
			return null;
		}
	}

}
