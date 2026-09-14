package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstVisitorTest {
	@Test
	void dispatchesEveryAstFamilyToTheConcreteNodeOverload() {
		AstVisitor<String> visitor = classNameVisitor();
		SourceLocation at = SourceLocation.of(1, 1);

		assertEquals("ThisObjectAstNode", new ThisObjectAstNode(at).accept(visitor));
		assertEquals("Question", new TryCatchAstNode.Question(at, new ThisObjectAstNode(at)).accept(visitor));
		assertEquals("ValueMatcherAstNode", new ValueMatcherAstNode(at, "value").accept(visitor));
		assertEquals("ConstantKeyFieldMatcher", new ObjectMatcherAstNode.ConstantKeyFieldMatcher(at, true, "value", null).accept(visitor));
		assertEquals("LabelAstNode", new LabelAstNode(at, "done").accept(visitor));
		assertEquals("AsBindingAstNode", new AsBindingAstNode(at, new ThisObjectAstNode(at), new ValueMatcherAstNode(at, "value")).accept(visitor));
		assertEquals("VariableKeyFieldConstruction", new ObjectConstructionAstNode.VariableKeyFieldConstruction(at, "value").accept(visitor));
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
