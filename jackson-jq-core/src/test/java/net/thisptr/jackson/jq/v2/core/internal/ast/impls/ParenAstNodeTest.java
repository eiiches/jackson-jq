package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ParenAstNodeTest {
	@Test
	void printersDoNotAddGroupingParentheses() throws JsonQueryException {
		assertPrintedAs("-1", "-1");
		assertPrintedAs("1 + 2", "1 + 2");
		assertPrintedAs(". | .", ". | .");
		assertPrintedAs("1, 2", "1, 2");
		assertPrintedAs("try . catch .", "try . catch .");
		assertPrintedAs(".foo?", ".foo?");
		assertPrintedAs("reduce .[] as $x (0; . + $x)", "reduce .[] as $x (0; . + $x)");
		assertPrintedAs("foreach .[] as $x (0; . + $x; .)", "foreach .[] as $x (0; . + $x; .)");
	}

	@Test
	void parserPreservesExplicitGroupingParentheses() throws JsonQueryException {
		assertPrintedAs("(1 + 2)", "(1 + 2)");
		assertPrintedAs("((1))", "((1))");
		assertPrintedAs("-(1)", "-(1)");
		assertPrintedAs("(.foo).bar?", "(.foo).bar?");
		assertPrintedAs("{value: (1, 2)}", "{value: (1, 2)}");

		assertInstanceOf(ParenAstNode.class, AstParser.parse("(1)", Versions.JQ_1_6));
	}

	@Test
	void requiredParenthesesRemainOwnedByTheirConstructs() throws JsonQueryException {
		assertPrintedAs("def f($arg): $arg; f(1)", "def f($arg): $arg; f(1)");
		assertPrintedAs("{(.foo): 1}", "{(.foo): 1}");
		assertPrintedAs(". as {(.key): $value} | $value", ". as {(.key): $value} | $value");
		assertPrintedAs("\"\\(.foo)\"", "\"\\(.foo)\"");
	}

	@Test
	void printedGroupingCanBeParsedAgain() throws JsonQueryException {
		AstNode parsed = AstParser.parse("((1 + 2) * 3) | (. - (4 - 5))", Versions.JQ_1_6);
		String printed = parsed.toString();
		assertEquals(printed, AstParser.parse(printed, Versions.JQ_1_6).toString());
	}

	@Test
	void parenthesizedExpressionsCompileTransparently() throws JsonQueryException {
		Environment<JsonNode> environment = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();
		JsonQuery<JsonNode> query = environment.compile("((1 + 2))");
		List<JsonNode> output = new ArrayList<>();
		query.apply(NullNode.getInstance(), output::add);
		assertEquals(3, output.get(0).intValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	void parenthesizedModuleMetadataRemainsConstant() throws JsonQueryException {
		TopLevelAstNode<JsonNode> topLevel = (TopLevelAstNode<JsonNode>) AstParser.parse("module ({name: \"test\"}); .", Versions.JQ_1_6);
		JsonNode metadata = topLevel.moduleDirective().getMetadata(Jackson2JsonProviderImpl.getInstance());
		assertEquals("test", metadata.get("name").textValue());
	}

	private static void assertPrintedAs(String query, String expected) throws JsonQueryException {
		assertEquals(expected, AstParser.parse(query, Versions.JQ_1_6).toString());
	}
}
