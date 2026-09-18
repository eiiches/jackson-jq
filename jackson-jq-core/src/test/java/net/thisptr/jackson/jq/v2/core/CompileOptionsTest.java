package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompileOptionsTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	private final List<Diagnostic> reported = new ArrayList<>();
	private final CompileOptions options = CompileOptions.newBuilder().setDiagnosticListener(reported::add).build();

	private Environment<JsonNode> environment() {
		return environment(Versions.JQ_1_7);
	}

	private static Environment<JsonNode> environment(Version version) {
		return EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, version).build();
	}

	@Test
	void tailCallOptimizationIsOnByDefaultAndCanBeTurnedOff() throws JsonQueryException {
		assertThat(CompileOptions.newBuilder().build().getOptimizationOptions().getTailCallOptimization()).isTrue();

		CompileOptions off = CompileOptions.newBuilder()
				.setOptimizationOptions(OptimizationOptions.newBuilder().setTailCallOptimization(false).build())
				.build();
		assertThat(off.getOptimizationOptions().getTailCallOptimization()).isFalse();

		// Both compile the same query to the same values; what differs is only what it costs the Java stack,
		// which TailCallTest covers.
		List<JsonNode> withTailCalls = new ArrayList<>();
		environment().compile("0 | def f: if . < 8 then . + 1 | f else . end; [f]").apply(JSON_PROVIDER.createNull(), withTailCalls::add);
		List<JsonNode> withoutTailCalls = new ArrayList<>();
		environment().compile("0 | def f: if . < 8 then . + 1 | f else . end; [f]", off).apply(JSON_PROVIDER.createNull(), withoutTailCalls::add);
		assertThat(withTailCalls).isEqualTo(withoutTailCalls);
	}

	@Test
	void warnsAboutACommaWrittenAsAnOperandOfAPipe() throws JsonQueryException {
		environment().compile("1, 2 | .", options);

		assertThat(reported).hasSize(1);
		Diagnostic diagnostic = reported.get(0);
		assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
		assertThat(diagnostic.message()).contains("`,` binds tighter than `|`").contains("write `(1, 2)`");
		assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 1, 1, 4));
	}

	@Test
	void warnsOnEitherSideOfThePipe() throws JsonQueryException {
		environment().compile(".[] | .a, .b", options);

		assertThat(reported).hasSize(1);
		assertThat(reported.get(0).location()).isEqualTo(SourceLocation.of(1, 7, 1, 12));
	}

	@Test
	void warnsOncePerUnparenthesisedComma() throws JsonQueryException {
		environment().compile("1, 2 | 3, 4 | .", options);

		assertThat(reported).hasSize(2);
		assertThat(reported).extracting(Diagnostic::location)
				.containsExactly(SourceLocation.of(1, 1, 1, 4), SourceLocation.of(1, 8, 1, 11));
	}

	@Test
	void warnsAboutWhatAPipeHeadScopes() throws JsonQueryException {
		environment().compile("1 as $x | 2, 3", options);
		environment().compile("label $out | 4, 5", options);

		assertThat(reported).extracting(Diagnostic::location)
				.containsExactly(SourceLocation.of(1, 11, 1, 14), SourceLocation.of(1, 14, 1, 17));
	}

	@Test
	void warnsAboutABindingPipeWrittenAfterAComma() throws JsonQueryException {
		environment().compile("1 + 1, 2 as $a | $a + 1", options);

		assertThat(reported).hasSize(1);
		Diagnostic diagnostic = reported.get(0);
		assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
		assertThat(diagnostic.message()).contains("`as` binds only `2`").contains("write `(2 as $a | $a + 1)`");
		assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 8, 1, 23));
	}

	// Before jq 1.8 a binding pipe outranks every ordinary operator, so it can hide under one that
	// looks like it encloses the binding: `1, 2 + 3 as $a | $a * 2` is `1, (2 + (3 as $a | $a * 2))`.
	@Test
	void warnsAboutABindingPipeHidingUnderAnOperator() throws JsonQueryException {
		environment().compile("1, 2 + 3 as $a | $a * 2", options);

		assertThat(reported).hasSize(1);
		assertThat(reported.get(0).message()).contains("`as` binds only `3`").contains("write `(3 as $a | $a * 2)`");
		assertThat(reported.get(0).location()).isEqualTo(SourceLocation.of(1, 8, 1, 23));
	}

	// The same source binds a different value per version, so the message names the value instead of
	// describing its position.
	@Test
	void namesTheValueTheVersionActuallyBinds() throws JsonQueryException {
		environment(Versions.JQ_1_7).compile("1, 2 + 3 as $a | $a * 2", options);
		environment(Versions.JQ_1_8_0).compile("1, 2 + 3 as $a | $a * 2", options);

		assertThat(reported).extracting(Diagnostic::message)
				.containsExactly(
						"`as` binds only `3`: write `(3 as $a | $a * 2)` to make the grouping explicit",
						"`as` binds only `2 + 3`: write `(2 + 3 as $a | $a * 2)` to make the grouping explicit");
	}

	// The same source groups differently per version, and the warning follows the grouping that is
	// actually surprising. Since jq 1.8 the binding takes `2 + 3`, which is how it reads already.
	@Test
	void followsTheVersionsOwnGrouping() throws JsonQueryException {
		environment(Versions.JQ_1_7).compile("2 + 3 as $a | $a * 2", options);
		assertThat(reported).extracting(Diagnostic::location).containsExactly(SourceLocation.of(1, 5, 1, 20));

		reported.clear();
		environment(Versions.JQ_1_8_0).compile("2 + 3 as $a | $a * 2", options);
		assertThat(reported).isEmpty();
	}

	// The `as` after the `,` and the `,` after the `|` are two independent grouping questions, so
	// each gets its own warning.
	@Test
	void warnsAboutEveryGroupingAmbiguityInOneExpression() throws JsonQueryException {
		environment().compile("1, 2 as $x | 3, 4", options);

		assertThat(reported).extracting(Diagnostic::location)
				.containsExactly(SourceLocation.of(1, 4, 1, 17), SourceLocation.of(1, 14, 1, 17));
	}

	@Test
	void walksIntoFunctionBodies() throws JsonQueryException {
		environment().compile("def f: 1, 2 | .; f", options);

		assertThat(reported).extracting(Diagnostic::location).containsExactly(SourceLocation.of(1, 8, 1, 11));
	}

	@Test
	void staysQuietWhenTheGroupingIsExplicit() throws JsonQueryException {
		environment().compile("(1, 2) | .", options);
		environment().compile("1 | (2, 3)", options);
		environment().compile("1 + 1, (2 as $a | $a + 1)", options);

		assertThat(reported).isEmpty();
	}

	@Test
	void staysQuietWhenThereIsNoPipeToBeAmbiguousWith() throws JsonQueryException {
		environment().compile("1, 2", options);
		environment().compile("[1, 2]", options);
		environment().compile("{a: 1, b: 2}", options);

		assertThat(reported).isEmpty();
	}

	// map/1 is written in jq as `def map(f): [.[] | f];`. Its body is the library's business, and
	// warnings about it would be noise the caller cannot act on.
	@Test
	void staysQuietAboutTheBuiltinsTheQueryHappensToCall() throws JsonQueryException {
		environment().compile("[1, 2] | map(. + 1) | add", options);

		assertThat(reported).isEmpty();
	}

	@Test
	void producesNoDiagnosticsWithoutAListener() throws JsonQueryException {
		environment().compile("1, 2 | .", CompileOptions.newBuilder().build());
		environment().compile("1, 2 | .");

		assertThat(reported).isEmpty();
	}

	@Test
	void canBeReusedForManyCompilations() throws JsonQueryException {
		environment().compile("1, 2 | .", options);
		assertThat(reported).hasSize(1);

		// The options are immutable, so the same object drives every later compilation identically.
		environment().compile("3, 4 | .", options);
		assertThat(reported).hasSize(2);
	}
	// --- constant folding ---------------------------------------------------------------------

	@Test
	void constantFoldingIsOnByDefaultAndBoundedByDefault() {
		ConstantFoldingOptions folding = CompileOptions.newBuilder().build().getOptimizationOptions().getConstantFoldingOptions();

		assertThat(folding.isEnabled()).isTrue();
		assertThat(folding.getMaxResults()).isEqualTo(256);

		// Not RuntimeOptions' own default, which bounds nothing: an unbounded compile-time evaluation
		// could run forever. See ConstantFoldingOptions.Builder#setRuntimeOptions.
		RuntimeOptions runtimeOptions = folding.getRuntimeOptions();
		assertThat(runtimeOptions.getMaxArrayLength()).isEqualTo(256);
		assertThat(runtimeOptions.getMaxObjectMemberCount()).isEqualTo(256);
		assertThat(runtimeOptions.getMaxStringLength()).isEqualTo(4096);
		assertThat(runtimeOptions.getMaxOutputsPerExpression()).isEqualTo(256);
		assertThat(runtimeOptions.getMaxUserDefinedFunctionCalls()).isEqualTo(256);
		assertThat(runtimeOptions).isNotEqualTo(RuntimeOptions.newBuilder().build());
	}

	@Test
	void optionsLeftAtTheirDefaultsShareOneInstance() {
		assertThat(CompileOptions.newBuilder().build()).isSameAs(CompileOptions.newBuilder().build());
		assertThat(OptimizationOptions.newBuilder().build()).isSameAs(OptimizationOptions.newBuilder().build());
		assertThat(ConstantFoldingOptions.newBuilder().build()).isSameAs(ConstantFoldingOptions.newBuilder().build());

		// Setting a folding option to its default value still yields the shared default.
		assertThat(CompileOptions.newBuilder()
				.setOptimizationOptions(OptimizationOptions.newBuilder()
						.setConstantFoldingOptions(ConstantFoldingOptions.newBuilder().build())
						.build())
				.build()).isSameAs(CompileOptions.newBuilder().build());

		assertThat(CompileOptions.newBuilder()
				.setOptimizationOptions(OptimizationOptions.newBuilder()
						.setConstantFoldingOptions(ConstantFoldingOptions.newBuilder().setMaxResults(8).build())
						.build())
				.build()).isNotSameAs(CompileOptions.newBuilder().build());
	}

	@Test
	void oneConstantFoldingOptionsCompilesAnyNumberOfQueries() throws JsonQueryException {
		CompileOptions options = CompileOptions.newBuilder()
				.setOptimizationOptions(OptimizationOptions.newBuilder()
						.setConstantFoldingOptions(ConstantFoldingOptions.newBuilder().setEnabled(false).build())
						.build())
				.build();
		Environment<JsonNode> env = environment();

		assertThat(env.compile("1 + 1", options)).isNotNull();
		assertThat(env.compile("2 + 2", options)).isNotNull();
		assertThat(options.getOptimizationOptions().getConstantFoldingOptions().isEnabled()).isFalse();
	}

	@Test
	void aNegativeMaxResultsIsRejected() {
		// The null guards on setRuntimeOptions/setOptimizationOptions/setConstantFoldingOptions are not asserted here: NullAway
		// rejects the call at compile time, so only a caller outside its reach can reach them.
		assertThatThrownBy(() -> ConstantFoldingOptions.newBuilder().setMaxResults(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maxResults");
	}

}
