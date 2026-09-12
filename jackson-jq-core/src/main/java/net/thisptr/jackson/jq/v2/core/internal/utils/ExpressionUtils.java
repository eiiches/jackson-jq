package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.CommaAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringLiteralAstNode;
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
			AstNode elements = ((ArrayConstructionAstNode) expr).q;
			if (elements == null)
				return jsonProvider.createArray(Collections.emptyList()); // empty

			List<JsonNode> result = new ArrayList<>();
			if (!collectLiteralElements(jsonProvider, elements, result))
				return null;

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

	/**
	 * Appends the values of a {@code ,}-separated element list to {@code out}, in source order.
	 * A {@code ,} is a left-nested binary node, so an explicit stack avoids consuming one Java stack
	 * frame per element. Parentheses are transparent, as they are during normal compilation.
	 *
	 * @return false if any element is not a constant, leaving {@code out} in an unspecified state
	 */
	private static <JsonNode> boolean collectLiteralElements(JsonProvider<JsonNode> jsonProvider, AstNode expr, List<JsonNode> out) {
		Deque<AstNode> pending = new ArrayDeque<>();
		pending.push(expr);
		while (!pending.isEmpty()) {
			AstNode element = pending.pop();
			if (element instanceof CommaAstNode) {
				CommaAstNode comma = (CommaAstNode) element;
				pending.push(comma.right());
				pending.push(comma.left());
				continue;
			}
			if (element instanceof ParenAstNode) {
				pending.push(((ParenAstNode) element).value());
				continue;
			}

			JsonNode value = evaluateLiteralExpression(jsonProvider, element);
			if (value == null)
				return false;
			out.add(value);
		}
		return true;
	}

}
