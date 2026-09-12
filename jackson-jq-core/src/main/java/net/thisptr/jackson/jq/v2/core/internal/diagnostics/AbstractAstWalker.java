package net.thisptr.jackson.jq.v2.core.internal.diagnostics;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstVisitor;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BreakExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ConditionalAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ForeachExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FormattingFilterAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.IdentifierFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NegativeExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.RecursionOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ReduceExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ValueMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;

/**
 * Visits every node of a parsed AST, depth first, in source order. Subclasses override only the
 * node kinds they care about and call {@code super} to keep descending.
 */
public abstract class AbstractAstWalker implements AstVisitor<Void> {

	/**
	 * Visits {@code node} and everything under it.
	 */
	public final void walk(AstNode node) {
		node.accept(this);
	}

	private void walkNullable(@Nullable AstNode node) {
		if (node != null)
			node.accept(this);
	}

	@Override
	public Void visit(ArrayConstructionAstNode node) {
		walkNullable(node.q);
		return null;
	}

	@Override
	public Void visit(ArrayMatcherAstNode node) {
		for (AstNode matcher : node.matchers())
			walk(matcher);
		return null;
	}

	@Override
	public Void visit(AsBindingAstNode node) {
		walk(node.value());
		walk(node.matcher());
		return null;
	}

	@Override
	public Void visit(BinaryOpAstNode node) {
		walk(node.lhs);
		walk(node.rhs);
		return null;
	}

	@Override
	public Void visit(BooleanLiteralAstNode node) {
		return null;
	}

	@Override
	public Void visit(BracketExtractFieldAccessAstNode node) {
		walk(node.target());
		return null;
	}

	@Override
	public Void visit(BracketFieldAccessAstNode node) {
		walk(node.target());
		walk(node.startExpr());
		walk(node.endExpr());
		return null;
	}

	@Override
	public Void visit(BreakExpressionAstNode node) {
		return null;
	}

	@Override
	public Void visit(ConditionalAstNode node) {
		for (Pair<AstNode, AstNode> branch : node.switches()) {
			walk(branch._1);
			walk(branch._2);
		}
		walk(node.otherwise());
		return null;
	}

	@Override
	public Void visit(ObjectMatcherAstNode.ConstantKeyFieldMatcher node) {
		walkNullable(node.matcher());
		return null;
	}

	@Override
	public Void visit(ObjectMatcherAstNode.ExpressionKeyFieldMatcher node) {
		walk(node.name());
		walk(node.matcher());
		return null;
	}

	@Override
	public Void visit(ForeachExpressionAstNode node) {
		walk(node.iterExpr());
		walk(node.matcher());
		walk(node.initExpr());
		walk(node.updateExpr());
		walkNullable(node.extractExpr());
		return null;
	}

	@Override
	public Void visit(FormattingFilterAstNode node) {
		return null;
	}

	@Override
	public Void visit(FunctionCallAstNode node) {
		for (AstNode arg : node.args())
			walk(arg);
		return null;
	}

	@Override
	public Void visit(FunctionDefinitionAstNode node) {
		walk(node.body());
		return null;
	}

	@Override
	public Void visit(ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst node) {
		walkNullable(node.value);
		return null;
	}

	@Override
	public Void visit(IdentifierFieldAccessAstNode node) {
		walk(node.target());
		return null;
	}

	@Override
	public Void visit(ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst node) {
		walk(node.key());
		walk(node.value());
		return null;
	}

	@Override
	public Void visit(LabelAstNode node) {
		return null;
	}

	@Override
	public Void visit(NegativeExpressionAstNode node) {
		walk(node.value());
		return null;
	}

	@Override
	public Void visit(NullLiteralAstNode node) {
		return null;
	}

	@Override
	public Void visit(NumericLiteralAstNode node) {
		return null;
	}

	@Override
	public Void visit(ObjectConstructionAstNode node) {
		for (ObjectConstructionAstNode.FieldConstructionAst field : node.fields)
			walk(field);
		return null;
	}

	@Override
	public Void visit(ObjectMatcherAstNode node) {
		for (ObjectMatcherAstNode.FieldMatcher matcher : node.matchers())
			walk(matcher);
		return null;
	}

	@Override
	public Void visit(ParenAstNode node) {
		walk(node.value());
		return null;
	}

	@Override
	public Void visit(RecursionOperatorAstNode node) {
		return null;
	}

	@Override
	public Void visit(ReduceExpressionAstNode node) {
		walk(node.iterExpr());
		walk(node.matcher());
		walk(node.initExpr());
		walk(node.reduceExpr());
		return null;
	}

	@Override
	public Void visit(SemicolonOperatorAstNode node) {
		for (AstNode expr : node.expressions())
			walk(expr);
		return null;
	}

	@Override
	public Void visit(StringFieldAccessAstNode node) {
		walk(node.target());
		walk(node.key());
		return null;
	}

	@Override
	public Void visit(StringInterpolationAstNode node) {
		walkNullable(node.formatter());
		for (Pair<Integer, AstNode> interpolation : node.interpolations())
			walk(interpolation._2);
		return null;
	}

	@Override
	public Void visit(StringLiteralAstNode node) {
		return null;
	}

	@Override
	public Void visit(ObjectConstructionAstNode.StringKeyFieldConstructionAst node) {
		walk(node.key);
		walkNullable(node.value);
		return null;
	}

	@Override
	public Void visit(ThisObjectAstNode node) {
		return null;
	}

	@Override
	public Void visit(TopLevelAstNode node) {
		TopLevelAstNode.ModuleDirective moduleDirective = node.moduleDirective();
		if (moduleDirective != null)
			walk(moduleDirective.metadataExpr());
		for (TopLevelAstNode.ImportStatement statement : node.imports())
			walkNullable(statement.metadataExpr());
		walk(node.expr());
		return null;
	}

	@Override
	public Void visit(TryCatchAstNode node) {
		walk(node.tryExpr());
		walkNullable(node.catchExpr());
		return null;
	}

	@Override
	public Void visit(TryCatchAstNode.Question node) {
		return visit((TryCatchAstNode) node);
	}

	@Override
	public Void visit(ValueMatcherAstNode node) {
		return null;
	}

	@Override
	public Void visit(ObjectConstructionAstNode.VariableKeyFieldConstruction node) {
		return null;
	}

	@Override
	public Void visit(VariableAccessAstNode node) {
		return null;
	}
}
