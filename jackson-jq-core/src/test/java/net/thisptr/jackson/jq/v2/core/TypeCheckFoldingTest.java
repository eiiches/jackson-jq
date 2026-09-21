package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Constant folding must not be observable to the type checker.
 * <p>
 * Folding runs only once type checking is done, so the checker reads the tree the query was written as.
 * These tests compile the same query with folding on and off and require the same answer from both:
 * anything else would mean an optimization decides what a query means.
 */
class TypeCheckFoldingTest {
	/**
	 * Queries whose arguments are constant, and so would be folded away before the checker saw them if
	 * folding ran first. Each pairs a jq query with the input type to check it against.
	 */
	private static final List<String> CONSTANT_ARGUMENT_QUERIES = List.of(
			"[limit(1 + 1; .)]",
			"[range(1 + 1)]",
			"ltrimstr(\"a\" + \"b\")",
			"has(\"a\" + \"b\")",
			"[.[] | select(true and true)]",
			"getpath([\"a\" + \"b\"])",
			"[paths(. == ({\"a\": 1} | .a))]");

	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
			.build();

	private static CompileOptions options(Type inputType, boolean folding, List<Diagnostic> diagnostics) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setInputType(inputType)
				.setDiagnosticListener(diagnostics::add)
				.setOptimizationOptions(OptimizationOptions.newBuilder()
						.setConstantFoldingOptions(ConstantFoldingOptions.newBuilder().setEnabled(folding).build())
						.build())
				.build();
	}

	private record Checked(FilterType type, List<String> diagnostics) {
	}

	private Checked check(String query, Type inputType, boolean folding) throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		FilterType type = environment.compile(query, options(inputType, folding, diagnostics)).getType();
		List<String> messages = new ArrayList<>();
		for (Diagnostic diagnostic : diagnostics)
			messages.add(diagnostic.severity() + ": " + diagnostic.message());
		return new Checked(type, messages);
	}

	private void assertFoldingIsInvisible(String query, Type inputType) throws JsonQueryException {
		assertThat(check(query, inputType, true))
				.describedAs("%s against %s", query, inputType)
				.isEqualTo(check(query, inputType, false));
	}

	@Test
	void foldingChangesNeitherTypeNorDiagnostics() throws JsonQueryException {
		for (String query : CONSTANT_ARGUMENT_QUERIES) {
			assertFoldingIsInvisible(query, AnyType.getInstance());
			assertFoldingIsInvisible(query, ArrayType.of(StringType.getInstance()));
		}
	}

	@Test
	void aConstantArgumentIsStillCheckedAgainstTheSignature() {
		// getpath/1 takes a path array; folding the argument first would hand the checker ANY and let it pass.
		CompileOptions strict = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(AnyType.getInstance())
				.build();
		assertThatThrownBy(() -> environment.compile("getpath(1 + 1)", strict))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void aWarningInsideAConstantArgumentSurvivesFolding() throws JsonQueryException {
		// The argument evaluates cleanly, so it folds; the closed-object warning inside it must remain.
		Checked folded = check("has({test: 1} | .fail | tostring)", ObjectType.of(), true);
		assertThat(folded.diagnostics()).singleElement().asString().contains("Field \"fail\"").contains("closed object");
		assertThat(folded).isEqualTo(check("has({test: 1} | .fail | tostring)", ObjectType.of(), false));
	}

	@Test
	void aConstantArgumentKeepsItsInferredType() throws JsonQueryException {
		// `explode` answers an array of numbers; the checker only knows that if it sees the call, not a fold.
		assertThat(check("ltrimstr(\"a\" + \"b\")", StringType.getInstance(), true).type().outputType())
				.isSameAs(StringType.getInstance());
		assertFoldingIsInvisible("[(\"ab\" | explode) | .[]]", StringType.getInstance());
	}
}
