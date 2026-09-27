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
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@code #jackson-jq:type} comment, on a {@code def} in the query being compiled. What the
 * comment states is what a caller is checked against and told, in place of whatever the body would
 * have been read as.
 */
class TypeAnnotationTest {
	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7).build();

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private Type outputOf(String query, Type inputType) throws JsonQueryException {
		return environment.compile(query, strict(inputType)).getType().outputType();
	}

	private Type outputOf(String query) throws JsonQueryException {
		return outputOf(query, StringType.getInstance());
	}

	@Test
	void aDefinitionAnswersWhatItStates() throws JsonQueryException {
		assertThat(outputOf("""
				#jackson-jq:type () => (STRING -> INT)
				def n: length;
				n"""))
				.isEqualTo(NumericType.of(NumberKind.INT));
	}

	/**
	 * The point of stating a signature: left alone, this body is read as the type of the one value it
	 * can answer, and the statement is what the caller reads instead.
	 */
	@Test
	void aStatementReplacesWhatTheBodyWouldHaveAnswered() throws JsonQueryException {
		assertThat(outputOf("""
				#jackson-jq:type () => (ANY -> STRING)
				def f: 1;
				f"""))
				.isSameAs(StringType.getInstance());
		assertThat(outputOf("""
				def f: 1;
				f""").toString())
				.isEqualTo("1");
	}

	/**
	 * And the other half of it: a body that would have accepted anything rejects what its definition
	 * says it is written for.
	 */
	@Test
	void aStatementRejectsAnInputItsBodyWouldHaveAccepted() throws JsonQueryException {
		String query = """
				#jackson-jq:type () => (STRING -> STRING)
				def f: .;
				f""";
		assertThat(outputOf(query, StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> outputOf(query, NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void aDefinitionTakingParametersStatesThemInTheFunctionForm() throws JsonQueryException {
		assertThat(outputOf("""
				#jackson-jq:type (STRING -> STRING; STRING -> INT) => (STRING -> [*:STRING])
				def f(a; b): [a, (b|tostring)];
				f("x"; 1)""").toString())
				.isEqualTo("[*:STRING]");
	}

	@Test
	void consecutiveStatementsAreOverloads() throws JsonQueryException {
		String definition = """
				#jackson-jq:type (ANY -> STRING) => (ANY -> STRING)
				#jackson-jq:type (ANY -> BOOLEAN) => (ANY -> BOOLEAN)
				def f(x): x;
				""";
		assertThat(outputOf(definition + "f(\"a\")")).isSameAs(StringType.getInstance());
		assertThat(outputOf(definition + "f(true)")).isSameAs(BooleanType.getInstance());
		assertThatThrownBy(() -> outputOf(definition + "f(1)"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	/**
	 * A quantified statement says what a definition does to whatever it is given, which is what an
	 * unquantified one cannot: the input travels to the parameter and the parameter's output back out.
	 */
	@Test
	void aStatementMayQuantifyOverItsOwnVariables() throws JsonQueryException {
		assertThat(outputOf("""
				#jackson-jq:type <I, T> (I -> T) => (I -> [T])
				def f(x): [x];
				f(.)""", BooleanType.getInstance()).toString())
				.isEqualTo("[BOOLEAN]");
		assertThat(outputOf("""
				#jackson-jq:type <T: STRING|BOOLEAN> () => (T -> [T])
				def w: [.];
				w""", BooleanType.getInstance()).toString())
				.isEqualTo("[BOOLEAN]");
	}

	@Test
	void aNestedDefinitionMayStateItsOwn() throws JsonQueryException {
		assertThat(outputOf("""
				def outer:
					#jackson-jq:type () => (STRING -> INT)
					def inner: length;
					inner;
				outer"""))
				.isEqualTo(NumericType.of(NumberKind.INT));
	}

	/**
	 * And a definition that states nothing is read from its body exactly as before -- {@code length}
	 * answers a number without saying which kind, where the statement above could say {@code INT}.
	 */
	@Test
	void aDefinitionWithoutAStatementIsStillReadFromItsBody() throws JsonQueryException {
		assertThat(outputOf("""
				def f: length;
				f""").toString())
				.isEqualTo("NUMBER");
	}

	/**
	 * A statement is taken as given, not checked against the body it stands in front of: the body is not
	 * analysed at all. What is an error in an unstated definition -- a call site analyses the body it is
	 * about to run, and this one cannot index a number -- goes unreported once the definition states a
	 * signature, and says nothing about the body either way.
	 */
	@Test
	void aStatementIsTakenAsGivenRatherThanCheckedAgainstItsBody() throws JsonQueryException {
		assertThatThrownBy(() -> outputOf("""
				def f: 1 | .a;
				f"""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");

		List<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThat(environment.compile("""
				#jackson-jq:type () => (ANY -> STRING)
				def f: 1 | .a;
				f""", options).getType().outputType())
				.isSameAs(StringType.getInstance());
		assertThat(diagnostics).isEmpty();
	}

	/**
	 * Nor does a definition that states a signature have anything to say about the input it was given,
	 * which is what the generic pass would otherwise invent a variable for and then complain about.
	 */
	@Test
	void aStatementLeavesNothingToSayAboutTheBody() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThat(environment.compile("""
				#jackson-jq:type () => (STRING -> NUMBER)
				def n: length;
				n""", options).getType().outputType())
				.isSameAs(NumericType.getInstance());
		assertThat(diagnostics).isEmpty();
	}

	@Test
	void aStatementOfTheWrongParameterCountIsRejected() {
		assertThatThrownBy(() -> outputOf("""
				#jackson-jq:type (ANY -> ANY) => (ANY -> ANY)
				def f: .;
				f"""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("states 1 parameter, but the definition it annotates takes 0.");
		assertThatThrownBy(() -> outputOf("""
				#jackson-jq:type () => (ANY -> ANY)
				def f(a): a;
				f(.)"""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("states 0 parameters, but the definition it annotates takes 1.");
	}

	@Test
	void textThatIsNotTheNotationIsRejected() {
		assertThatThrownBy(() -> outputOf("""
				#jackson-jq:type () => (STRING -> nonsense!)
				def f: .;
				f"""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("#jackson-jq:type")
				.hasMessageContaining("is not a type scheme");
	}

	/**
	 * A comment that opens with the marker and then says something unreadable is a mistake, not a
	 * comment: reporting it is the whole reason the marker is matched ahead of {@code <COMMENT>}.
	 */
	@Test
	void aMisspeltDirectiveIsReportedRatherThanPassedOver() {
		assertThatThrownBy(() -> outputOf("""
				#jackson-jq:types STRING -> STRING
				def f: .;
				f"""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("is not a type scheme");
	}

	@Test
	void aStatementMustStandInFrontOfADefinition() {
		assertThatThrownBy(() -> outputOf("""
				#jackson-jq:type () => (STRING -> STRING)
				."""))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("syntax error");
	}

	@Test
	void anOrdinaryCommentIsStillJustAComment() throws JsonQueryException {
		assertThat(outputOf("""
				# type: STRING -> INT
				# jackson-jq:type STRING -> INT
				def f: .;
				f"""))
				.isSameAs(StringType.getInstance());
	}

	/**
	 * Checking is what reads the statement, so a query compiled without it is unaffected by one.
	 */
	@Test
	void aStatementIsInertWhenCheckingIsOff() throws JsonQueryException {
		assertThat(environment.compile("""
				#jackson-jq:type () => (STRING -> STRING)
				def f: .;
				f""").getType().outputType())
				.isSameAs(AnyType.getInstance());
	}
}
