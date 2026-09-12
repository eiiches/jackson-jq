package net.thisptr.jackson.jq.v2.core.internal.compile;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ValueMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An {@code as} binding and a {@code label} are pipe heads: what they scope is the right-hand side
 * of the {@code |} they head, so they only compile as a {@link net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode}'s
 * left side. The grammar rejects a head that no {@code |} follows, so these cover the compiler's own
 * guard against an AST built by hand -- which the fuzzer does.
 */
class PipeHeadCompilationTest {
	private static final SourceLocation AT = SourceLocation.of(1, 1);

	private static Environment<JsonNode> environment() {
		return new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();
	}

	@Test
	void aBindingOutsideAPipeIsRejected() {
		AsBindingAstNode binding = new AsBindingAstNode(AT, new ThisObjectAstNode(AT), new ValueMatcherAstNode(AT, "x"));
		assertThatThrownBy(() -> Compiler.compile(environment(), binding))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("`. as $x` must be followed by `|`");
	}

	@Test
	void aLabelOutsideAPipeIsRejected() {
		assertThatThrownBy(() -> Compiler.compile(environment(), new LabelAstNode(AT, "out")))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("`label $out` must be followed by `|`");
	}
}
