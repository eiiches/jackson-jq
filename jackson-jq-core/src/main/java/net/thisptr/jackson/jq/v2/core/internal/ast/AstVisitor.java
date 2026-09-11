package net.thisptr.jackson.jq.v2.core.internal.ast;

/**
 * Performs a type-specific operation on parser AST nodes.
 *
 * <p>Every AST construct uses the same result type so visitors can operate uniformly across the
 * complete tree.
 */
public interface AstVisitor<R> {
	R visit(ArrayConstructionAstNode node);

	R visit(ArrayMatcherAstNode node);

	R visit(BinaryOpAstNode node);

	R visit(BooleanLiteralAstNode node);

	R visit(BracketExtractFieldAccessAstNode node);

	R visit(BracketFieldAccessAstNode node);

	R visit(BreakExpressionAstNode node);

	R visit(ConditionalAstNode node);

	R visit(ObjectMatcherAstNode.ConstantKeyFieldMatcher node);

	R visit(ObjectMatcherAstNode.ExpressionKeyFieldMatcher node);

	R visit(ForeachExpressionAstNode node);

	R visit(FormattingFilterAstNode node);

	R visit(FunctionCallAstNode node);

	R visit(FunctionDefinitionAstNode node);

	R visit(ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst node);

	R visit(IdentifierFieldAccessAstNode node);

	R visit(ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst node);

	R visit(LabelAstNode node);

	R visit(NegativeExpressionAstNode node);

	R visit(NullLiteralAstNode node);

	R visit(NumericLiteralAstNode node);

	R visit(ObjectConstructionAstNode node);

	R visit(ObjectMatcherAstNode node);

	R visit(ParenAstNode node);

	R visit(PipedQueryAstNode node);

	R visit(RecursionOperatorAstNode node);

	R visit(ReduceExpressionAstNode node);

	R visit(SemicolonOperatorAstNode node);

	R visit(StringFieldAccessAstNode node);

	R visit(StringInterpolationAstNode node);

	R visit(StringLiteralAstNode node);

	R visit(ObjectConstructionAstNode.StringKeyFieldConstructionAst node);

	R visit(ThisObjectAstNode node);

	R visit(TopLevelAstNode node);

	R visit(TryCatchAstNode node);

	R visit(TryCatchAstNode.Question node);

	R visit(TupleAstNode node);

	R visit(ValueMatcherAstNode node);

	R visit(ObjectConstructionAstNode.VariableKeyFieldConstruction node);

	R visit(VariableAccessAstNode node);

	R visit(VariableBindingAstNode node);
}
