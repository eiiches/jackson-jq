package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

import static org.assertj.core.api.Assertions.assertThat;

class AstVisitorTest {
	@Test
	void dispatchesEveryAstFamilyToTheConcreteNodeOverload() {
		AstVisitor<String> visitor = classNameVisitor();
		SourceLocation at = SourceLocation.of(1, 1);

		assertThat(new ThisObjectAstNode(at).accept(visitor)).isEqualTo("ThisObjectAstNode");
		assertThat(new TryCatchAstNode.Question(at, new ThisObjectAstNode(at)).accept(visitor)).isEqualTo("Question");
		assertThat(new ValueMatcherAstNode(at, "value").accept(visitor)).isEqualTo("ValueMatcherAstNode");
		assertThat(new ObjectMatcherAstNode.ConstantKeyFieldMatcher(at, true, "value", null).accept(visitor)).isEqualTo("ConstantKeyFieldMatcher");
		assertThat(new LabelAstNode(at, "done").accept(visitor)).isEqualTo("LabelAstNode");
		assertThat(new AsBindingAstNode(at, new ThisObjectAstNode(at), new ValueMatcherAstNode(at, "value")).accept(visitor)).isEqualTo("AsBindingAstNode");
		assertThat(new ObjectConstructionAstNode.VariableKeyFieldConstruction(at, "value").accept(visitor)).isEqualTo("VariableKeyFieldConstruction");
	}

	// The proxy implements the single visitor interface dynamically so this test can focus on
	// accept's double dispatch instead of repeating every visitor method as test boilerplate.
	@SuppressWarnings("unchecked")
	private static AstVisitor<String> classNameVisitor() {
		return (AstVisitor<String>) Proxy.newProxyInstance(
				AstVisitor.class.getClassLoader(),
				new Class<?>[] { AstVisitor.class },
				(proxy, method, args) -> args[0].getClass().getSimpleName());
	}
}
