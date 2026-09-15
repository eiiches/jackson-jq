package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FunctionSignatureAstNodeTest {
	@Test
	void parserStoresDefinitionSignature() throws JsonQueryException {
		SemicolonOperatorAstNode sequence = assertInstanceOf(SemicolonOperatorAstNode.class, parse("def f(a; $b): .; ."));
		FunctionDefinitionAstNode definition = assertInstanceOf(FunctionDefinitionAstNode.class, sequence.expressions().get(0));

		assertThat(definition.signature()).isEqualTo(FunctionSignature.of("f", 2));
		assertThat(definition.toString()).isEqualTo("def f(a; $b): .");
	}

	@Test
	void parserStoresCallSignatures() throws JsonQueryException {
		FunctionCallAstNode call = assertInstanceOf(FunctionCallAstNode.class, parse("f(.; .)"));
		assertThat(call.signature()).isEqualTo(FunctionSignature.of("f", 2));
		assertThat(call.moduleName()).isNull();

		FunctionCallAstNode qualifiedCall = assertInstanceOf(FunctionCallAstNode.class, parse("m::f(.)"));
		assertThat(qualifiedCall.signature()).isEqualTo(FunctionSignature.of("f", 1));
		assertThat(qualifiedCall.moduleName()).isEqualTo("m");
		assertThat(qualifiedCall.toString()).isEqualTo("m::f(.)");
	}

	@Test
	void parserStoresFormattingFilterSignature() throws JsonQueryException {
		FormattingFilterAstNode formatter = assertInstanceOf(FormattingFilterAstNode.class, parse("@csv"));

		assertThat(formatter.signature()).isEqualTo(FunctionSignature.of("@csv", 0));
		assertThat(formatter.toString()).isEqualTo("@csv");
	}

	private static AstNode parse(String query) throws JsonQueryException {
		return AstParser.parse(query, Versions.JQ_1_8_2);
	}
}
