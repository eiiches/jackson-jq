package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionSignatureAstNodeTest {
	@Test
	void parserStoresDefinitionSignature() throws JsonQueryException {
		AstNode parsed = parse("def f(a; $b): .; .");
		assertThat(parsed).isInstanceOf(SemicolonOperatorAstNode.class);
		SemicolonOperatorAstNode sequence = (SemicolonOperatorAstNode) parsed;
		assertThat(sequence.expressions().get(0)).isInstanceOf(FunctionDefinitionAstNode.class);
		FunctionDefinitionAstNode definition = (FunctionDefinitionAstNode) sequence.expressions().get(0);

		assertThat(definition.signature()).isEqualTo(FunctionSignature.of("f", 2));
		assertThat(definition).hasToString("def f(a; $b): .");
	}

	@Test
	void parserStoresCallSignatures() throws JsonQueryException {
		AstNode parsed = parse("f(.; .)");
		assertThat(parsed).isInstanceOf(FunctionCallAstNode.class);
		FunctionCallAstNode call = (FunctionCallAstNode) parsed;
		assertThat(call.signature()).isEqualTo(FunctionSignature.of("f", 2));
		assertThat(call.moduleName()).isNull();

		AstNode parsedQualified = parse("m::f(.)");
		assertThat(parsedQualified).isInstanceOf(FunctionCallAstNode.class);
		FunctionCallAstNode qualifiedCall = (FunctionCallAstNode) parsedQualified;
		assertThat(qualifiedCall.signature()).isEqualTo(FunctionSignature.of("f", 1));
		assertThat(qualifiedCall.moduleName()).isEqualTo("m");
		assertThat(qualifiedCall).hasToString("m::f(.)");
	}

	@Test
	void parserStoresFormattingFilterSignature() throws JsonQueryException {
		AstNode parsed = parse("@csv");
		assertThat(parsed).isInstanceOf(FormattingFilterAstNode.class);
		FormattingFilterAstNode formatter = (FormattingFilterAstNode) parsed;

		assertThat(formatter.signature()).isEqualTo(FunctionSignature.of("@csv", 0));
		assertThat(formatter).hasToString("@csv");
	}

	private static AstNode parse(String query) throws JsonQueryException {
		return AstParser.parse(query, Versions.JQ_1_8_2);
	}
}
