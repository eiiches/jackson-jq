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

import static org.assertj.core.api.Assertions.assertThat;

class CompileOptionsTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	private final List<Diagnostic> reported = new ArrayList<>();
	private final CompileOptions options = new CompileOptions().setDiagnosticListener(reported::add);

	private Environment<JsonNode> environment() {
		return new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7).build();
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

	// The value an `as` binding matches sits left of the `|`, so it is reported before what the
	// binding scopes -- the warnings stay in source order.
	@Test
	void warnsOnBothSidesOfAPipeHeadInSourceOrder() throws JsonQueryException {
		environment().compile("1, 2 as $x | 3, 4", options);

		assertThat(reported).extracting(Diagnostic::location)
				.containsExactly(SourceLocation.of(1, 1, 1, 4), SourceLocation.of(1, 14, 1, 17));
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
