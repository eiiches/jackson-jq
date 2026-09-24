package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeCheckTest {
	private final Environment<JsonNode> environment = EnvironmentBuilder
			.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
			.build();

	@Test
	void checkingIsOffByDefault() throws JsonQueryException {
		assertThat(environment.compile(".").getType()).isEqualTo(FilterType.of(AnyType.getInstance(), AnyType.getInstance()));
	}

	@Test
	void infersIdentityAndComposition() throws JsonQueryException {
		CompileOptions options = strict(StringType.getInstance());
		assertThat(environment.compile(".", options).getType()).isEqualTo(FilterType.of(StringType.getInstance(), StringType.getInstance()));
		assertThat(environment.compile("[.]", options).getType())
				.isEqualTo(FilterType.of(StringType.getInstance(), ArrayType.of(List.of(StringType.getInstance()))));
		assertThat(environment.compile(". | length", options).getType()).isEqualTo(FilterType.of(StringType.getInstance(), NumericType.getInstance()));
	}

	@Test
	void everyUnionAlternativeMustBeValid() {
		CompileOptions options = strict(UnionType.of(NumericType.getInstance(), ObjectType.of("a", StringType.getInstance())));
		assertThatThrownBy(() -> environment.compile(".a", options))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void warnModeRecoversWithAny() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setInputType(NumericType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		JsonQuery<JsonNode> query = environment.compile(".a", options);
		assertThat(query.getType().outputType()).isSameAs(AnyType.getInstance());
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).contains("Cannot index NUMBER");
			assertThat(diagnostic.location()).isNotNull();
		});
	}

	@Test
	void overloadWarningNamesJavaFunctionAndCallArity() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
		environment.compile("[1,2,3] | ltrimstr(\"1\")", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).isEqualTo("No overload of ltrimstr/1 accepts input [INT,INT,INT]"
						+ "\nAccepted types:\n  Input: STRING -> ltrimstr(STRING -> STRING) -> Output: STRING"));
	}

	@Test
	void overloadWarningExplainsArgumentTypeMismatch() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
		environment.compile("\"test\" | ltrimstr([1])", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message()).isEqualTo("Argument 1 of ltrimstr/1 has type [INT]; expected STRING"
					+ "\nAccepted types:\n  Input: STRING -> ltrimstr(STRING -> STRING) -> Output: STRING");
			assertThat(diagnostic.location()).isNotNull();
			assertThat(Objects.requireNonNull(diagnostic.location()).beginColumn()).isEqualTo(19);
		});
	}

	@Test
	void strictModeRejectsArgumentTypeMismatch() {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThatThrownBy(() -> environment.compile("\"test\" | ltrimstr([1])", options))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
		assertThat(diagnostics).singleElement().extracting(Diagnostic::severity).isEqualTo(Diagnostic.Severity.ERROR);
	}

	@Test
	void ambiguousOverloadWarningListsAllSchemesWithoutPickingOneExpectedType() throws JsonQueryException {
		JqFunction ambiguous = JqFunction.of("ambiguous", List.of(FunctionParameter.ofValue("arg")), ".",
				List.of(
						TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance(),
								FilterType.of(StringType.getInstance(), StringType.getInstance()))),
						TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance(),
								FilterType.of(StringType.getInstance(), BooleanType.getInstance())))));
		Environment<JsonNode> customEnvironment = EnvironmentBuilder
				.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.defineJqFunction(ambiguous)
				.build();
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
		customEnvironment.compile("\"test\" | ambiguous([1])", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).isEqualTo("No overload of ambiguous/1 matches this call with input \"test\""
						+ "\nAccepted types:"
						+ "\n  Input: STRING -> ambiguous(STRING -> STRING) -> Output: STRING"
						+ "\n  Input: STRING -> ambiguous(STRING -> BOOLEAN) -> Output: STRING"));
	}

	@Test
	void overloadWarningNamesJqFunctionAndCallArity() throws JsonQueryException {
		JqFunction stringOnly = JqFunction.of("string_only", List.of(), ".",
				List.of(TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance()))));
		Environment<JsonNode> customEnvironment = EnvironmentBuilder
				.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.defineJqFunction(stringOnly)
				.build();
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
		customEnvironment.compile("1 | string_only", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).isEqualTo("No overload of string_only/0 accepts input INT"
						+ "\nAccepted types:\n  Input: STRING -> string_only() -> Output: STRING"));
	}

	@Test
	void acceptedTypesShowBoundedVariablesAndMultipleArguments() throws JsonQueryException {
		TypeVariable value = TypeVariable.of("T");
		JqFunction combine = JqFunction.of("combine", List.of(FunctionParameter.ofFilter("left"), FunctionParameter.ofFilter("right")), ".",
				List.of(TypeScheme.of(Map.of(value, NumericType.getInstance()),
						FunctionType.of(value, value, FilterType.of(value, value), FilterType.of(value, StringType.getInstance())))));
		Environment<JsonNode> customEnvironment = EnvironmentBuilder
				.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.defineJqFunction(combine)
				.build();
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
		customEnvironment.compile("\"test\" | combine(.; .)", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).isEqualTo("No overload of combine/2 accepts input \"test\""
						+ "\nAccepted types:\n  <T: NUMBER> Input: T -> combine(T -> T; T -> STRING) -> Output: T"));
	}

	@Test
	void nonexistentFieldOfClosedObjectIsAWarning() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setDiagnosticListener(diagnostics::add)
				.build();
		JsonQuery<JsonNode> query = environment.compile("{test: 1} | .fail", options);
		assertThat(query.getType().outputType()).isSameAs(NullType.getInstance());
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).contains("Field \"fail\"").contains("closed object");
		});
	}

	@Test
	void constantQuotedObjectKeysAreDeclaredFields() throws JsonQueryException {
		Type expected = ObjectType.of(
				"a", NumericType.of(NumberKind.INT),
				"b", StringType.of("x"));
		assertThat(environment.compile("{a: 1, b: \"x\"}", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(expected);
		assertThat(environment.compile("{\"a\": 1, \"b\": \"x\"}", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(expected);
		assertThat(environment.compile("{\"a\": 1, \"b\": \"x\"} | .a | floor", strict(NullType.getInstance()))
				.getType().outputType()).isSameAs(NumericType.getInstance());
	}

	@Test
	void constantQuotedObjectKeyShorthandReadsTheInputField() throws JsonQueryException {
		Type input = ObjectType.of("a", StringType.getInstance());
		assertThat(environment.compile("{\"a\"}", strict(input)).getType().outputType()).isEqualTo(input);
	}

	@Test
	void quotedObjectKeysRestoreClosedObjectWarnings() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setDiagnosticListener(diagnostics::add)
				.build();
		environment.compile("{\"test\": 1} | .fail", options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).contains("Field \"fail\"").contains("closed object"));
	}

	@Test
	void constantExpressionObjectKeysBecomeDeclaredFields() throws JsonQueryException {
		Type input = ObjectType.of("key", StringType.getInstance());
		Type expected = ObjectType.of(Map.of("a", NumericType.of(NumberKind.INT)), StringType.of("x"));
		assertThat(environment.compile("{\"a\": 1, \"\\(.key)\": \"x\"}", strict(input)).getType().outputType())
				.isEqualTo(expected);
		assertThat(environment.compile("{(\"a\"): 1}", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(ObjectType.of("a", NumericType.of(NumberKind.INT)));
	}

	@Test
	void handlesEveryBinaryOperatorOverload() throws JsonQueryException {
		assertThat(environment.compile("[1, 2] - [2]", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
		assertThat(environment.compile("\"x\" * 2", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(UnionType.of(StringType.getInstance(), NullType.getInstance()));
		assertThat(environment.compile("\"a,b\" / \",\"", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(ArrayType.of(StringType.getInstance()));
	}

	@Test
	void validatesObjectKeysAndSliceBounds() {
		assertThatThrownBy(() -> environment.compile("{(1): 2}", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> environment.compile("\"abc\"[true:]", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void destructuringBindsExtractedTypes() throws JsonQueryException {
		assertThat(environment.compile("{a: 1} as {a: $x} | $x", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("{a: 1} as {a: $x} | $x | floor", strict(NullType.getInstance())).getType().outputType())
				.isSameAs(NumericType.getInstance());
		assertThatThrownBy(() -> environment.compile("1 as [$x] | 0", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void loopsAndAssignmentsCheckTheirChildren() throws JsonQueryException {
		assertThat(environment.compile("reduce [1, 2][] as $x (0; . + $x)", strict(NullType.getInstance())).getType().outputType())
				.isSameAs(NumericType.getInstance());
		assertThatThrownBy(() -> environment.compile("[1] | reduce .[] as $x (0; .a)", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> environment.compile("1 | (. = .a)", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void infersLoopsAssignmentsRecursionAndControlFlow() throws JsonQueryException {
		assertThat(environment.compile("foreach [1, 2][] as $x (0; . + $x; .)", strict(NullType.getInstance()))
				.getType().outputType()).isSameAs(NumericType.getInstance());
		assertThat(environment.compile(".a = \"x\"", strict(ObjectType.of("a", NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ObjectType.of("a", StringType.of("x")));
		assertThat(environment.compile(".a |= tostring", strict(ObjectType.of("a", NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ObjectType.of("a", StringType.getInstance()));
		assertThat(environment.compile("..", strict(ObjectType.of("a", NumericType.getInstance())))
				.getType().outputType()).isEqualTo(UnionType.of(ObjectType.of("a", NumericType.getInstance()), NumericType.getInstance()));
		assertThat(environment.compile("label $done | (1, break $done)", strict(NullType.getInstance()))
				.getType().outputType()).isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile(".a = empty", strict(ObjectType.of(AnyType.getInstance()))).getType().outputType())
				.isSameAs(NeverType.getInstance());
		assertThatThrownBy(() -> environment.compile(".a |= empty", strict(ObjectType.of(AnyType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void preservesDynamicObjectValueAndKnownBracketTypes() throws JsonQueryException {
		assertThat(environment.compile("{(.): 1}", strict(StringType.getInstance())).getType().outputType())
				.isEqualTo(ObjectType.of(Map.of(), NumericType.of(NumberKind.INT)));
		assertThat(environment.compile(".[(\"a\")]", strict(ObjectType.of("a", StringType.getInstance())))
				.getType().outputType()).isSameAs(StringType.getInstance());
	}

	@Test
	void quotedFieldAccessReadsTheFieldNotTheKey() throws JsonQueryException {
		Type input = ObjectType.of("a", NumericType.getInstance());
		assertThat(environment.compile(".\"a\"", strict(input)).getType().outputType()).isSameAs(NumericType.getInstance());
		assertThatThrownBy(() -> environment.compile(".\"a\".b", strict(input)))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void indexingRangesOverBothOperands() throws JsonQueryException {
		// `(1, 2)` is one expression producing two numbers, so the index type is a union; each alternative
		// has to meet each alternative of the target on its own.
		assertThat(environment.compile(".[(1, 2)]", strict(ArrayType.of(StringType.getInstance()))).getType().outputType())
				.isEqualTo(UnionType.of(StringType.getInstance(), NullType.getInstance()));
		assertThatThrownBy(() -> environment.compile(".[(1, \"a\")]", strict(ArrayType.of(StringType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void iteratingAnObjectKeepsItsFieldTypes() throws JsonQueryException {
		Type input = ObjectType.of("a", NumericType.getInstance(), "b", StringType.getInstance());
		assertThat(environment.compile(".[]", strict(input)).getType().outputType())
				.isEqualTo(UnionType.of(NumericType.getInstance(), StringType.getInstance()));
	}

	@Test
	void jqWrittenBuiltinsAreTypedFromTheirBodies() throws JsonQueryException {
		assertThat(environment.compile("map(tostring)", strict(ArrayType.of(NumericType.getInstance()))).getType().outputType())
				.isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(environment.compile("[.[] | select(. > 1)]", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ArrayType.of(NumericType.getInstance()));
		// An argument is the caller's own code wherever it ends up running, so its errors are still theirs.
		assertThatThrownBy(() -> environment.compile("map(.a)", strict(ArrayType.of(NumericType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void findingsInsideABuiltinsBodyRejectTheQuery() throws JsonQueryException {
		// `limit` starts its accumulator at `[$n, null]`, which the model spells as an array that knows what
		// is at each position, so the `.[0] - 1` that follows subtracts from the count it really is.
		assertThat(environment.compile("[limit(3; 1, 2, 3)]", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
		assertThat(environment.compile("flatten", strict(ArrayType.of(AnyType.getInstance()))).getType().outputType())
				.isNotNull();
		// `map` on a number fails at run time, and the analysis saying so is the same one that no longer
		// raises a false alarm on `limit`, so the caller hears it.
		assertThatThrownBy(() -> environment.compile("map(.)", strict(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void aBuiltinDeclaringItsInputIsCheckedAgainstThatRatherThanItsBody() {
		// `combinations` picks one element out of each of its input's arrays. Its own body is recursive and
		// slices, which says far less about what it accepts than the declaration does.
		assertThatThrownBy(() -> environment.compile("combinations", strict(ArrayType.of(NumericType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
		assertThatCode(() -> environment.compile("combinations", strict(ArrayType.of(ArrayType.of(NumericType.getInstance())))))
				.doesNotThrowAnyException();
		assertThatCode(() -> environment.compile("combinations", strict(AnyType.getInstance()))).doesNotThrowAnyException();
	}

	@Test
	void aDeclarationStandsInForABodyThisAnalysisCannotRead() throws JsonQueryException {
		// `_flatten` recurses under `if $i | type == "array"`, a guard that says which branch runs and which
		// this analysis cannot read -- so the body alone rejects `[1, 2] | flatten`, which is ordinary jq.
		assertThat(environment.compile("flatten", strict(ArrayType.of(NumericType.getInstance()))).getType().outputType())
				.isEqualTo(ArrayType.of(AnyType.getInstance()));
		assertThat(environment.compile("flatten(1)", strict(ArrayType.of(ArrayType.of(NumericType.getInstance()))))
				.getType().outputType()).isEqualTo(ArrayType.of(AnyType.getInstance()));
		// Declaring the input still rejects what really does fail.
		assertThatThrownBy(() -> environment.compile("flatten", strict(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void advisoryDiagnosticsAboutBuiltinBodiesReachTheCaller() throws JsonQueryException {
		// `walk` has a branch per container kind, so for a number all but one are unreachable -- something
		// the caller did not write and cannot change, and hears about anyway now that nothing is held back
		// for being inside a builtin.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(NumericType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThat(environment.compile("walk(.)", options).getType().outputType()).isNotNull();
		assertThat(diagnostics).isNotEmpty().allSatisfy(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			// Advisory or not, it is about a body the caller cannot see, so it points at the call they wrote.
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 1, 1, 7));
		});
	}

	@Test
	void aBuiltinBodysFailureIsReportedAtTheCallSiteWithACallTrace() throws JsonQueryException {
		// `map` is written in jq as `def map(f): [.[] | f];`, so what fails is a `.[]` the caller never wrote
		// and cannot be pointed at. The call they did write is where they have to look.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("\"test\" | map(\"test\")", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message()).isEqualTo("Cannot iterate over \"test\"\n  in map/1");
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 10, 1, 20));
		});
	}

	@Test
	void aDeclaredFunctionFailingInsideABuiltinBodyIsReportedAtTheCallSiteToo() throws JsonQueryException {
		// `unique` is `group_by(.) | map(.[0])`, and `group_by` is a Java function that states what it takes.
		// The statement is what rejects the call, from a body two levels below the one the caller named.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("\"test\" | unique", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message())
					.startsWith("No overload of group_by/1 accepts input \"test\"\nAccepted types:\n  ")
					.endsWith("\n  in group_by/1\n  in unique/0");
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 10, 1, 15));
		});
	}

	@Test
	void anArgumentTheCallerWroteIsNeverBlamedOnTheBuiltinItRunsIn() throws JsonQueryException {
		// `.a` runs inside map's body, but the caller wrote it, so it is reported where they wrote it and the
		// trace says nothing: naming map/1 would send them to look at a body that is doing what it should.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("[1, 2] | map(.a)", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message()).isEqualTo("Cannot index INT with a string");
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 14, 1, 15));
		});
	}

	@Test
	void aDefTheQueryWroteKeepsItsOwnLocationAndStillNamesItself() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("def f: .[]; \"test\" | f", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message()).isEqualTo("Cannot iterate over \"test\"\n  in f/0");
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 8, 1, 10));
		});
	}

	@Test
	void aCallTheQueryWroteItselfCarriesNoTrace() throws JsonQueryException {
		// The message already names the function, and the location is the call. There is nothing to trace.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("\"test\" | ltrimstr([1])", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).doesNotContain("\n  in "));
	}

	@Test
	void aRegisteredJqFunctionsOwnLocationsAreNeverReportedAsTheQuerys() throws JsonQueryException {
		// A jq function the environment registered shares the compilation's locations map, so its body does
		// carry line and column numbers -- measured against its own source text. Reporting one would put a
		// caret on whatever the query happens to have at that position.
		Environment<JsonNode> customEnvironment = EnvironmentBuilder
				.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.defineJqFunction(JqFunction.of("my_map", List.of(FunctionParameter.ofFilter("f")), "[.[] | f]"))
				.defineJqFunction(JqFunction.of("outer", List.of(), "my_map(.)"))
				.build();
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		customEnvironment.compile("\"test\" | outer", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.message()).isEqualTo("Cannot iterate over \"test\"\n  in my_map/1\n  in outer/0");
			assertThat(diagnostic.location()).isEqualTo(SourceLocation.of(1, 10, 1, 14));
		});
	}

	@Test
	void aTraceNamesARecursionOnceAndStopsAtTheCallsNearestTheFailure() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("def a: .[]; def b: a; def c: b; def d: c; def e: d;"
				+ " def g: e; def h: g; def i: h; def j: i; \"test\" | j", warn(diagnostics));
		assertThat(diagnostics).singleElement().satisfies(diagnostic ->
				assertThat(diagnostic.message()).isEqualTo("Cannot iterate over \"test\""
						+ "\n  in a/0\n  in b/0\n  in c/0\n  in d/0\n  in e/0\n  in g/0\n  in h/0\n  in i/0"
						+ "\n  ... 1 more"));
		// `walk` reaches itself for every container it finds, and says so once.
		ArrayList<Diagnostic> recursive = new ArrayList<>();
		environment.compile("\"test\" | walk(.)", warn(recursive));
		assertThat(recursive).isNotEmpty().allSatisfy(diagnostic ->
				assertThat(diagnostic.message()).endsWith("\n  in walk/1"));
	}

	@Test
	void recurseDeclaresTheEveryValueToEveryValueItReallyIs() throws JsonQueryException {
		// Its body walks the input's shape, but only by analysing a descent the caller did not write. `..`
		// is the same operation and keeps that precision, because the compiler answers it directly.
		assertThat(environment.compile("[recurse]", strict(ArrayType.of(NumericType.getInstance()))).getType().outputType())
				.isEqualTo(ArrayType.of(AnyType.getInstance()));
		assertThat(environment.compile("[..]", strict(ArrayType.of(NumericType.getInstance()))).getType().outputType())
				.isEqualTo(ArrayType.of(UnionType.of(ArrayType.of(NumericType.getInstance()), NumericType.getInstance())));
	}

	@Test
	void definitionsAreSpecializedPerCallSiteAndMemoized() throws JsonQueryException {
		assertThat(environment.compile("def f: .a; f", strict(ObjectType.of("a", NumericType.getInstance())))
				.getType().outputType()).isSameAs(NumericType.getInstance());
		assertThat(environment.compile("def m(g): [.[] | g]; [1, 2] | m(. + 1)", strict(NullType.getInstance()))
				.getType().outputType()).isEqualTo(ArrayType.of(NumericType.getInstance()));
		assertThat(environment.compile("def f($n): $n + 1; 2 | f(3)", strict(NullType.getInstance()))
				.getType().outputType()).isSameAs(NumericType.getInstance());
		// Nesting would multiply without the memo: this is 27 call sites over three bodies.
		assertThat(environment.compile("def a: .; def b: a|a|a; def c: b|b|b; c", strict(StringType.getInstance()))
				.getType().outputType()).isSameAs(StringType.getInstance());
	}

	@Test
	void recursiveDefinitionsAreIteratedToAFixedPoint() throws JsonQueryException {
		// A def reaches itself through its own closure, and the first round has no answer for the recursive
		// call yet, so the body is re-run until the answer stops changing.
		assertThat(environment.compile("def fact: if . <= 1 then 1 else . * (. - 1 | fact) end; fact",
				strict(NumericType.getInstance())).getType().outputType()).isSameAs(NumericType.getInstance());
		// jq spells mutual recursion by nesting, since a body cannot name a def declared after it.
		assertThat(environment.compile("def even: def odd: if . <= 0 then false else . - 1 | even end;"
						+ " if . <= 0 then true else . - 1 | odd end; even",
				strict(NumericType.getInstance())).getType().outputType()).isSameAs(BooleanType.getInstance());
	}

	@Test
	void growingAccumulatorsSettleIntoRecursiveTypes() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(ArrayType.of(NumericType.getInstance()))
				.setDiagnosticListener(diagnostics::add)
				.build();
		// Each iteration nests the accumulator one level deeper, which only a recursive type can spell.
		Type accumulator = environment.compile("reduce .[] as $x (null; {v: .})", options).getType().outputType();
		assertThat(accumulator).isInstanceOf(RecursiveType.class);
		TypeVariable loop = ((RecursiveType) accumulator).variable();
		assertThat(accumulator).isEqualTo(RecursiveType.of(loop, UnionType.of(NullType.getInstance(), ObjectType.of("v", loop))));
		assertThat(diagnostics).isEmpty();
		// A type-changing accumulator settles into the union of what it can hold.
		assertThat(environment.compile("reduce .[] as $x (0; tostring)", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance()));
		// An update that can emit nothing leaves the accumulator null.
		assertThat(environment.compile("reduce .[] as $x (0; empty)", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(UnionType.of(NumericType.of(NumberKind.INT), NullType.getInstance()));
	}

	@Test
	void assignmentsReachEveryPathTheSelectorProduces() throws JsonQueryException {
		Type input = ObjectType.of("a", NumericType.getInstance(), "b", StringType.getInstance());
		assertThat(environment.compile("(.a, .b) |= tostring", strict(input)).getType().outputType())
				.isEqualTo(ObjectType.of("a", StringType.getInstance(), "b", StringType.getInstance()));
		assertThat(environment.compile(".[] |= tostring", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(environment.compile(".[0:1] = [\"x\"]", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.of("x"))));
		assertThatThrownBy(() -> environment.compile(".[0:1] = \"x\"", strict(ArrayType.of(NumericType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void selectorsThatCannotBeFollowedStillCheckTheirChildren() {
		// The type of `(.[] | select(...))`'s paths cannot be written down, but `select`'s own argument is
		// an ordinary expression and its errors are the query's.
		assertThatThrownBy(() -> environment.compile("(.[] | select(.a)) |= tostring",
				strict(ArrayType.of(NumericType.getInstance()))))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void widenedSelectorsKeepWhatTheyDoNotReach() throws JsonQueryException {
		// `select` picks this position or nothing, so the element is the old value or the new one -- and
		// the array around it is untouched either way.
		assertThat(environment.compile("(.[] | select(. > 1)) |= tostring", strict(ArrayType.of(NumericType.getInstance())))
				.getType().outputType()).isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
	}

	@Test
	void duplicateConstantObjectKeysUseTheLastValueType() throws JsonQueryException {
		assertThat(environment.compile("{\"a\": 1, \"a\": \"x\"}", strict(NullType.getInstance())).getType().outputType())
				.isEqualTo(ObjectType.of("a", StringType.of("x")));
	}

	@Test
	void narrowsTypeTests() throws JsonQueryException {
		CompileOptions options = strict(UnionType.of(NumericType.getInstance(), StringType.getInstance()));
		JsonQuery<JsonNode> query = environment.compile("if type == \"number\" then . - 1 else 0 end", options);
		assertThat(query.getType().outputType()).isSameAs(NumericType.getInstance());
	}

	@Test
	void narrowsTypeTestsThroughSelect() throws JsonQueryException {
		// The form jq is actually written in. Inside `select`, the condition is the parameter `pred`, so
		// nothing about how the test is spelled is visible where the branch is chosen; what narrows is
		// asking what `pred` answers for one alternative of the input at a time.
		CompileOptions options = strict(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance()));
		assertThat(environment.compile("select(type == \"number\")", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("select(type != \"number\")", options).getType().outputType())
				.isSameAs(StringType.getInstance());
	}

	@Test
	void narrowsTypeTestsWrittenAsAPredicateFunction() throws JsonQueryException {
		// `isnan` answers false outright for anything that is not a number, which is the whole of the test.
		CompileOptions options = strict(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance()));
		assertThat(environment.compile("select(isnan)", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
	}

	@Test
	void narrowsTypeTestsCombinedWithBooleanOperators() throws JsonQueryException {
		CompileOptions options = strict(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance(),
				BooleanType.getInstance()));
		assertThat(environment.compile("select(type == \"number\" or type == \"string\")", options)
				.getType().outputType())
				.isEqualTo(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance()));
	}

	@Test
	void narrowsTypeTestsThroughADefinitionTheQueryWrote() throws JsonQueryException {
		// The string being compared is not written at the comparison at all, so no rule reading the tree
		// could find it. The type of what `wanted` produces is the one known string, and that is enough.
		CompileOptions options = strict(UnionType.of(NumericType.of(NumberKind.INT), StringType.getInstance()));
		assertThat(environment.compile("def wanted: \"number\"; select(type == wanted)", options)
				.getType().outputType()).isEqualTo(NumericType.of(NumberKind.INT));
	}

	@Test
	void narrowsAnUnreachableTypeTestToNothing() throws JsonQueryException {
		// A filter that nothing can get through emits nothing, and the branch it would have emitted from
		// is dead code the query wrote -- reported at the call, since the branch itself is `select`'s.
		List<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setInputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThat(environment.compile("select(type == \"number\")", options).getType().outputType())
				.isSameAs(NeverType.getInstance());
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).isEqualTo("Branch is unreachable for input type STRING\n  in select/1");
		});
	}

	@Test
	void infersAndInstantiatesPolymorphicDefinitions() throws JsonQueryException {
		JsonQuery<JsonNode> query = environment.compile("def identity: .; (1 | identity), (\"x\" | identity)", strict(NullType.getInstance()));
		assertThat(query.getType().outputType()).isEqualTo(UnionType.of(NumericType.of(NumberKind.INT), StringType.of("x")));
	}

	@Test
	void arrayIndexFindsSubsequencePositions() throws JsonQueryException {
		JsonQuery<JsonNode> query = environment.compile("[1, 2] | .[[1]]", strict(NullType.getInstance()));
		assertThat(query.getType().outputType()).isEqualTo(ArrayType.of(NumericType.getInstance()));

		JsonQuery<JsonNode> generic = environment.compile(".[[\"needle\"]]", strict(ArrayType.of(StringType.getInstance())));
		assertThat(generic.getType().outputType()).isEqualTo(ArrayType.of(NumericType.getInstance()));

		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions mismatchOptions = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(NullType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		JsonQuery<JsonNode> mismatch = environment.compile("[\"a\", \"b\"] | .[[1]]", mismatchOptions);
		assertThat(mismatch.getType().outputType()).isEqualTo(ArrayType.of(NumericType.getInstance()));
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).contains("INT").contains("\"a\"|\"b\"");
		});
		assertThatThrownBy(() -> environment.compile("\"x\" | .[[1]]", strict(NullType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void nestedDefinitionsSeeWhatTheyCaptured() throws JsonQueryException {
		// g captures $x from the def enclosing it, and a call site analysing f's body knows what $x is
		// there, so g does too -- even though g is called with a different input than f was.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		JsonQuery<JsonNode> query = environment.compile("def f: . as $x | def g: $x; (1 | g); f", options);
		assertThat(query.getType().outputType()).isSameAs(StringType.getInstance());
		assertThat(diagnostics).isEmpty();
	}

	@Test
	void definitionSignaturesDoNotLeakAnEnclosingDefinitionsVariables() throws JsonQueryException {
		// The signature the generic pass infers for g cannot speak for f's input variable: variables compare
		// by name, so a scheme quantifying it would capture f's own. Nothing g says about it may escape.
		assertThat(environment.compile("def f: . as $x | def g: $x; (1 | g); f", strict(NumericType.getInstance()))
				.getType().outputType()).isSameAs(NumericType.getInstance());
	}

	@Test
	void definitionsAreSpecializedAtLocalCallSites() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		assertThatThrownBy(() -> environment.compile("def increment: . + 1; increment", options))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
		assertThat(environment.compile("def increment: . + 1; increment", strict(NumericType.getInstance())).getType().outputType())
				.isSameAs(NumericType.getInstance());
		assertThat(diagnostics).allSatisfy(diagnostic ->
				assertThat(diagnostic.severity()).isIn(Diagnostic.Severity.WARNING, Diagnostic.Severity.ERROR));
	}

	@Test
	void guardedFailureIsAWarningAndNeverEmits() throws JsonQueryException {
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(NumericType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();
		JsonQuery<JsonNode> query = environment.compile(".a?", options);
		assertThat(query.getType().outputType()).isSameAs(NeverType.getInstance());
		assertThat(diagnostics).singleElement().extracting(Diagnostic::severity).isEqualTo(Diagnostic.Severity.WARNING);
	}

	// --- declared output type -----------------------------------------------------------------

	@Test
	void strictRejectsAResultTheDeclaredOutputTypeDoesNotAccept() {
		assertThatThrownBy(() -> environment.compile("1 + 1", strictOutput(StringType.getInstance())))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("Type checking failed");
	}

	@Test
	void warnsAboutAResultTheDeclaredOutputTypeDoesNotAccept() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("1 + 1", warnOutput(StringType.getInstance(), diagnostics));

		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).isEqualTo("Output type NUMBER is not assignable to the declared output type STRING");
		});
	}

	@Test
	void saysNothingAboutAResultTheDeclaredOutputTypeAccepts() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("\"a\"", warnOutput(StringType.getInstance(), diagnostics));

		assertThat(diagnostics).isEmpty();
	}

	// A union is accepted only if every alternative is, so the alternative that is not is the one worth
	// naming: the reader would otherwise compare two unions member by member to find it.
	@Test
	void namesOnlyTheAlternativeTheDeclaredOutputTypeTurnsDown() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		Type declared = UnionType.of(StringType.getInstance(), NumericType.of(NumberKind.INT));
		environment.compile("1, \"a\", true", warnOutput(declared, diagnostics));

		assertThat(diagnostics).singleElement().extracting(Diagnostic::message)
				.isEqualTo("Output type true is not assignable to the declared output type " + declared);
	}

	// ANY is what the analysis produces when it cannot pin the result down, and it matches in both
	// directions. So a declaration faults results known not to fit, never results not known to fit.
	@Test
	void anInferredAnyFitsEveryDeclaredOutputType() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile(".", warnOutput(StringType.getInstance(), diagnostics));

		assertThat(diagnostics).isEmpty();
	}

	// With checking off nothing is inferred, so there is no result to hold the declaration against.
	@Test
	void theDeclaredOutputTypeIsInertWhenCheckingIsOff() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.OFF)
				.setOutputType(StringType.getInstance())
				.setDiagnosticListener(diagnostics::add)
				.build();

		assertThat(environment.compile("1 + 1", options).getType().outputType()).isSameAs(AnyType.getInstance());
		assertThat(diagnostics).isEmpty();
	}

	// The declaration is checked against, not inferred from: the compiled query keeps reporting the
	// type the analysis actually arrived at, which is the more precise of the two.
	@Test
	void theCompiledQueryReportsTheInferredOutputTypeNotTheDeclaredOne() throws JsonQueryException {
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setOutputType(UnionType.of(StringType.getInstance(), NumericType.of(NumberKind.INT)))
				.build();

		assertThat(environment.compile("1", options).getType().outputType()).isEqualTo(NumericType.of(NumberKind.INT));
	}

	@Test
	void typeFilterNarrowingInFunctionDefinitions() throws JsonQueryException {
		List<Diagnostic> diagnostics = new ArrayList<>();
		environment.compile("def f($a): $a | numbers + 1; f(\"test\")", warn(diagnostics));
		assertThat(diagnostics).isEmpty();

		diagnostics.clear();
		environment.compile("def f($a): $a | strings + \"x\"; f(123)", warn(diagnostics));
		assertThat(diagnostics).isEmpty();

		diagnostics.clear();
		environment.compile("def f($a): $a | arrays | .[0] + 1; f([1, 2])", warn(diagnostics));
		assertThat(diagnostics).isEmpty();
	}

	@Test
	void addInfersPreciseOutputForKnownArrayInputs() throws JsonQueryException {
		CompileOptions options = strict(NullType.getInstance());
		assertThat(environment.compile("[1, 2, 3] | add", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("[\"a\", \"b\"] | add", options).getType().outputType())
				.isSameAs(StringType.getInstance());
		assertThat(environment.compile("[[1], [2]] | add", options).getType().outputType())
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
		assertThat(environment.compile("[{\"a\": 1}, {\"b\": 2}] | add", options).getType().outputType())
				.isEqualTo(ObjectType.of(AnyType.getInstance()));
		assertThat(environment.compile("[] | add", options).getType().outputType())
				.isSameAs(NullType.getInstance());
	}

	@Test
	void minAndMaxInferPreciseOutputForKnownArrayInputs() throws JsonQueryException {
		CompileOptions options = strict(NullType.getInstance());
		assertThat(environment.compile("[1, 2, 3] | min", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("[1, 2, 3] | max", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("[\"b\", \"a\"] | min", options).getType().outputType())
				.isEqualTo(UnionType.of(StringType.of("a"), StringType.of("b")));
		assertThat(environment.compile("[\"b\", \"a\"] | max", options).getType().outputType())
				.isEqualTo(UnionType.of(StringType.of("a"), StringType.of("b")));

		assertThat(environment.compile("[1, 2, 3] | min_by(.)", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("[1, 2, 3] | max_by(.)", options).getType().outputType())
				.isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(environment.compile("[\"b\", \"a\"] | min_by(.)", options).getType().outputType())
				.isEqualTo(UnionType.of(StringType.of("a"), StringType.of("b")));
		assertThat(environment.compile("[\"b\", \"a\"] | max_by(.)", options).getType().outputType())
				.isEqualTo(UnionType.of(StringType.of("a"), StringType.of("b")));

		assertThat(environment.compile("[] | min", options).getType().outputType())
				.isSameAs(NullType.getInstance());
		assertThat(environment.compile("[] | max", options).getType().outputType())
				.isSameAs(NullType.getInstance());
	}

	private static CompileOptions strictOutput(Type outputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setOutputType(outputType)
				.build();
	}

	private static CompileOptions warnOutput(Type outputType, List<Diagnostic> diagnostics) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setOutputType(outputType)
				.setDiagnosticListener(diagnostics::add)
				.build();
	}

	private static CompileOptions warn(List<Diagnostic> diagnostics) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setDiagnosticListener(diagnostics::add)
				.build();
	}

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}
}
