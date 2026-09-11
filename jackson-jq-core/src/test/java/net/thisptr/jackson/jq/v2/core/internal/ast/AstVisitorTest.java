package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstVisitorTest {
	@Test
	void dispatchesEveryAstFamilyToTheConcreteNodeOverload() {
		AstVisitor<String> visitor = classNameVisitor();

		assertEquals("ThisObjectAstNode", new ThisObjectAstNode().accept(visitor));
		assertEquals("Question", new TryCatchAstNode.Question(new ThisObjectAstNode()).accept(visitor));
		assertEquals("ValueMatcherAstNode", new ValueMatcherAstNode("value").accept(visitor));
		assertEquals("ConstantKeyFieldMatcher", new ObjectMatcherAstNode.ConstantKeyFieldMatcher(true, "value", null).accept(visitor));
		assertEquals("LabelAstNode", new LabelAstNode("done", new ThisObjectAstNode()).accept(visitor));
		assertEquals("VariableBindingAstNode", new VariableBindingAstNode(new ThisObjectAstNode(), new ValueMatcherAstNode("value"), new ThisObjectAstNode()).accept(visitor));
		assertEquals("VariableKeyFieldConstruction", new ObjectConstructionAstNode.VariableKeyFieldConstruction("value").accept(visitor));
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
