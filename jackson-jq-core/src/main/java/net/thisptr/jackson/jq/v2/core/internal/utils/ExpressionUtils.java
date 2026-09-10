package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class ExpressionUtils {

	/**
	 * Evaluates a literal expression.
	 *
	 * @param jsonProvider the JSON provider
	 * @param expr the AST node to evaluate
	 * @return null if expr is not a constant
	 */
	public static <JsonNode> @Nullable JsonNode evaluateLiteralExpression(JsonProvider<JsonNode> jsonProvider, AstNode expr) {
		if (expr instanceof ParenAstNode) {
			return evaluateLiteralExpression(jsonProvider, ((ParenAstNode) expr).value());
		} else if (expr instanceof ObjectConstructionAstNode) {
			Map<String, JsonNode> fields = new LinkedHashMap<>();

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

					fields.put(k, v);
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

					fields.put(k, v);
				} else {
					return null;
				}
			}

			return jsonProvider.createObject(fields);
		} else if (expr instanceof ArrayConstructionAstNode) {
			List<JsonNode> result = new ArrayList<>();

			AstNode tuple = ((ArrayConstructionAstNode) expr).q;
			if (tuple == null)
				return jsonProvider.createArray(Collections.emptyList()); // empty

			if (tuple instanceof TupleAstNode) {
				List<AstNode> values = ((TupleAstNode) tuple).qs;
				for (AstNode valueExpr : values) {
					JsonNode value = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (value == null)
						return null;

					result.add(value);
				}
			} else {
				JsonNode value = evaluateLiteralExpression(jsonProvider, tuple);
				if (value == null)
					return null;
				result.add(value);
			}

			return jsonProvider.createArray(result);
		} else if (expr instanceof BooleanLiteralAstNode) {
			return jsonProvider.createBoolean(((BooleanLiteralAstNode) expr).value());
		} else if (expr instanceof NullLiteralAstNode) {
			return jsonProvider.createNull();
		} else if (expr instanceof NumericLiteralAstNode) {
			return jsonProvider.createNumber(new BigDecimal(((NumericLiteralAstNode) expr).text()));
		} else if (expr instanceof StringLiteralAstNode) {
			return jsonProvider.createString(((StringLiteralAstNode) expr).value());
		} else {
			return null;
		}
	}

}
