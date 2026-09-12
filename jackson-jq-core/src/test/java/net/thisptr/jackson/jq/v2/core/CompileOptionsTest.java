package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

class CompileOptionsTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	private final List<Diagnostic> reported = new ArrayList<>();
	private final CompileOptions options = new CompileOptions().setDiagnosticListener(reported::add);

	private Environment<JsonNode> environment() {
		return environment(Versions.JQ_1_7);
	}

	private static Environment<JsonNode> environment(Version version) {
		return new EnvironmentBuilder<>(JSON_PROVIDER, version).build();
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
		environment().compile("1, 2 | .", new CompileOptions());
		environment().compile("1, 2 | .");

		assertThat(reported).isEmpty();
	}

	@Test
	void diagnosesModuleSourceToo() throws JsonQueryException {
		environment().compileModule("def f: 1, 2 | .;", options);

		assertThat(reported).extracting(Diagnostic::location).containsExactly(SourceLocation.of(1, 8, 1, 11));
	}

	@Test
	void readsItsSettingsOnceAtCompileTime() throws JsonQueryException {
		environment().compile("1, 2 | .", options);
		assertThat(reported).hasSize(1);

		// Reusing the same options object for a second query is fine, and silencing it afterwards
		// does not retroactively unreport anything.
		options.setDiagnosticListener(null);
		environment().compile("3, 4 | .", options);
		assertThat(reported).hasSize(1);
	}
}
