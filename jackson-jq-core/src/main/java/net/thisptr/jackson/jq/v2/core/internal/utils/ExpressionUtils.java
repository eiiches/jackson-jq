package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;

public class ExpressionUtils {

	/**
	 * Evaluates a literal expression.
	 *
	 * @param jsonProvider the JSON provider
	 * @param expr the AST node to evaluate
	 * @return the constant value, or {@link Maybe#absent()} if expr is not a constant
	 */
	public static <JsonNode> Maybe<JsonNode> evaluateLiteralExpression(JsonProvider<JsonNode> jsonProvider, AstNode expr) {
		if (expr instanceof ParenAstNode paren) {
			return evaluateLiteralExpression(jsonProvider, paren.value());
		} else if (expr instanceof ObjectConstructionAstNode obj) {
			Map<String, JsonNode> fields = new LinkedHashMap<>();

			for (ObjectConstructionAstNode.FieldConstructionAst field : obj.fields) {
				if (field instanceof ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst f) {
					String k = f.key;
					AstNode valueExpr = f.value;

					if (valueExpr == null) // this field depends on input and is not a constant
						return Maybe.absent();

					Maybe<JsonNode> v = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (v.isAbsent())
						return Maybe.absent();

					fields.put(k, v.get());
				} else if (field instanceof ObjectConstructionAstNode.StringKeyFieldConstructionAst f) {
					AstNode valueExpr = f.value;
					if (!(f.key instanceof StringLiteralAstNode keyStr)) // then the key is string interpolation and not a constant
						return Maybe.absent();
					if (valueExpr == null) // this field depends on input and is not a constant
						return Maybe.absent();
					String k = keyStr.value();

					Maybe<JsonNode> v = evaluateLiteralExpression(jsonProvider, valueExpr);
					if (v.isAbsent())
						return Maybe.absent();

					fields.put(k, v.get());
				} else {
					return Maybe.absent();
				}
			}

			return Maybe.of(jsonProvider.createObject(fields));
		} else if (expr instanceof ArrayConstructionAstNode arr) {
			AstNode elements = arr.q;
			if (elements == null)
				return Maybe.of(jsonProvider.createArray(Collections.emptyList())); // empty

			List<JsonNode> result = new ArrayList<>();
			if (!collectLiteralElements(jsonProvider, elements, result))
				return Maybe.absent();

			return Maybe.of(jsonProvider.createArray(result));
		} else if (expr instanceof BooleanLiteralAstNode bool) {
			return Maybe.of(jsonProvider.createBoolean(bool.value()));
		} else if (expr instanceof NullLiteralAstNode) {
			return Maybe.of(jsonProvider.createNull());
		} else if (expr instanceof NumericLiteralAstNode num) {
			return Maybe.of(jsonProvider.createNumber(new BigDecimal(num.text())));
		} else if (expr instanceof StringLiteralAstNode str) {
			return Maybe.of(jsonProvider.createString(str.value()));
		} else {
			return Maybe.absent();
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
			if (element instanceof BinaryOpAstNode comma && comma.operator == BinaryOperator.COMMA) {
				pending.push(comma.rhs);
				pending.push(comma.lhs);
				continue;
			}
			if (element instanceof ParenAstNode paren) {
				pending.push(paren.value());
				continue;
			}

			Maybe<JsonNode> value = evaluateLiteralExpression(jsonProvider, element);
			if (value.isAbsent())
				return false;
			out.add(value.get());
		}
		return true;
	}

}
