package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
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
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The signatures the builtin {@code Function} implementations publish, seen through the types
 * {@code TypeCheck} infers for calls to them.
 */
class BuiltinTypeSchemeTest {
	private final Environment<JsonNode> environment = environment(Versions.JQ_1_7);

	private static Environment<JsonNode> environment(Version jqVersion) {
		return EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), jqVersion).build();
	}

	private static CompileOptions strict(Type inputType) {
		return CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.STRICT)
				.setInputType(inputType)
				.build();
	}

	private Type outputOf(String query, Type inputType) throws JsonQueryException {
		return environment.compile(query, strict(inputType)).getType().outputType();
	}

	@Test
	void narrowsTheOutputOfZeroArgumentBuiltins() throws JsonQueryException {
		assertThat(outputOf("explode", StringType.getInstance())).isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
		assertThat(outputOf("implode", ArrayType.of(NumericType.getInstance()))).isSameAs(StringType.getInstance());
		assertThat(outputOf("utf8bytelength", StringType.getInstance())).isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(outputOf("tostring", BooleanType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("fromjson", StringType.getInstance())).isSameAs(AnyType.getInstance());
		assertThat(outputOf("not", AnyType.getInstance())).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("builtins", NullType.getInstance())).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(outputOf("@base64", NumericType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("@csv", ArrayType.of(AnyType.getInstance()))).isSameAs(StringType.getInstance());
		assertThat(outputOf("sqrt", NumericType.getInstance())).isSameAs(NumericType.getInstance());
	}

	@Test
	void typeFiltersNarrowOutputAndPreserveContainerTypes() throws JsonQueryException {
		assertThat(outputOf("numbers", AnyType.getInstance())).isSameAs(NumericType.getInstance());
		assertThat(outputOf("numbers", NumericType.of(NumberKind.INT))).isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(outputOf("numbers", StringType.getInstance())).isSameAs(NumericType.getInstance());
		assertThat(outputOf("arrays", ArrayType.of(StringType.getInstance()))).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(outputOf("arrays", UnionType.of(ArrayType.of(StringType.getInstance()), NullType.getInstance())))
				.isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(outputOf("arrays", StringType.getInstance())).isEqualTo(ArrayType.of(AnyType.getInstance()));
		assertThat(outputOf("objects", ObjectType.of("a", StringType.getInstance()))).isEqualTo(ObjectType.of("a", StringType.getInstance()));
		assertThat(outputOf("objects", StringType.getInstance())).isEqualTo(ObjectType.of(AnyType.getInstance()));
		assertThat(outputOf("iterables", ArrayType.of(StringType.getInstance()))).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(outputOf("iterables", ObjectType.of("a", StringType.getInstance()))).isEqualTo(ObjectType.of("a", StringType.getInstance()));
		assertThat(outputOf("scalars", UnionType.of(StringType.getInstance(), ArrayType.of(NumericType.getInstance()))))
				.isSameAs(StringType.getInstance());
		assertThat(outputOf("values", UnionType.of(StringType.getInstance(), NullType.getInstance())))
				.isSameAs(StringType.getInstance());
		assertThat(outputOf("strings", AnyType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("booleans", AnyType.getInstance())).isSameAs(BooleanType.getInstance());
		assertThat(outputOf("nulls", AnyType.getInstance())).isSameAs(NullType.getInstance());
		assertThat(outputOf("finites", AnyType.getInstance())).isSameAs(NumericType.getInstance());
		assertThat(outputOf("normals", AnyType.getInstance())).isSameAs(NumericType.getInstance());
	}

	@Test
	void keysDependOnWhetherTheInputIsAnObjectOrAnArray() throws JsonQueryException {
		assertThat(outputOf("keys", ObjectType.of(AnyType.getInstance()))).isEqualTo(ArrayType.of(StringType.getInstance()));
		assertThat(outputOf("keys_unsorted", ArrayType.of(StringType.getInstance())))
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
	}

	@Test
	void toEntriesDescribesTheEntryObject() throws JsonQueryException {
		assertThat(outputOf("to_entries", ObjectType.of(AnyType.getInstance())))
				.isEqualTo(ArrayType.of(ObjectType.of("key", StringType.getInstance(), "value", AnyType.getInstance())));
		assertThat(outputOf("to_entries", ObjectType.of(NumericType.getInstance())))
				.isEqualTo(ArrayType.of(ObjectType.of("key", StringType.getInstance(), "value", NumericType.getInstance())));
		assertThatThrownBy(() -> outputOf("to_entries", ArrayType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void elementTypesFlowThroughArrayBuiltins() throws JsonQueryException {
		Type strings = ArrayType.of(StringType.getInstance());
		assertThat(outputOf("sort_by(.)", strings)).isEqualTo(strings);
		assertThat(outputOf("group_by(.)", strings)).isEqualTo(ArrayType.of(strings));
		assertThat(outputOf("min_by(.)", strings)).isEqualTo(UnionType.of(StringType.getInstance(), NullType.getInstance()));
		assertThat(outputOf("max_by(.)", strings)).isEqualTo(UnionType.of(StringType.getInstance(), NullType.getInstance()));
	}

	@Test
	void addNarrowsOutputForNonEmptyArraysAndRejectsMixed() throws JsonQueryException {
		Type ints = NumericType.of(NumberKind.INT);
		Type strings = StringType.getInstance();
		Type numbers = NumericType.getInstance();
		Type objects = ObjectType.of(AnyType.getInstance());

		// Non-empty numbers
		assertThat(outputOf("add", ArrayType.of(List.of(ints, ints, ints)))).isEqualTo(ints);
		assertThat(outputOf("add", ArrayType.of(List.of(numbers, ints)))).isEqualTo(numbers);

		// Non-empty strings
		assertThat(outputOf("add", ArrayType.of(List.of(strings, strings)))).isEqualTo(strings);

		// Non-empty arrays (concatenation)
		assertThat(outputOf("add", ArrayType.of(List.of(ArrayType.of(ints), ArrayType.of(ints)))))
				.isEqualTo(ArrayType.of(ints));

		// Non-empty objects (merging)
		assertThat(outputOf("add", ArrayType.of(List.of(objects, objects)))).isEqualTo(objects);

		// Empty array
		assertThat(outputOf("add", ArrayType.of(List.of()))).isEqualTo(NullType.getInstance());

		// Open arrays
		assertThat(outputOf("add", ArrayType.of(numbers))).isEqualTo(UnionType.of(numbers, NullType.getInstance()));
		assertThat(outputOf("add", ArrayType.of(strings))).isEqualTo(UnionType.of(strings, NullType.getInstance()));
		assertThat(outputOf("add", ArrayType.of(ArrayType.of(ints))))
				.isEqualTo(UnionType.of(ArrayType.of(ints), NullType.getInstance()));
		assertThat(outputOf("add", ArrayType.of(objects))).isEqualTo(UnionType.of(objects, NullType.getInstance()));

		// Mixed elements rejected
		assertThatThrownBy(() -> outputOf("add", ArrayType.of(List.of(ints, strings))))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void minAndMaxNarrowOutputForNonEmptyArrays() throws JsonQueryException {
		Type ints = NumericType.of(NumberKind.INT);
		Type strings = StringType.getInstance();
		Type numbers = NumericType.getInstance();

		// Non-empty arrays for min/max
		assertThat(outputOf("min", ArrayType.of(List.of(ints, ints)))).isEqualTo(ints);
		assertThat(outputOf("max", ArrayType.of(List.of(ints, ints)))).isEqualTo(ints);
		assertThat(outputOf("min", ArrayType.of(List.of(strings, strings)))).isEqualTo(strings);
		assertThat(outputOf("max", ArrayType.of(List.of(strings, strings)))).isEqualTo(strings);

		// Non-empty arrays for min_by/max_by
		assertThat(outputOf("min_by(.)", ArrayType.of(List.of(ints, ints)))).isEqualTo(ints);
		assertThat(outputOf("max_by(.)", ArrayType.of(List.of(ints, ints)))).isEqualTo(ints);
		assertThat(outputOf("min_by(.)", ArrayType.of(List.of(strings, strings)))).isEqualTo(strings);
		assertThat(outputOf("max_by(.)", ArrayType.of(List.of(strings, strings)))).isEqualTo(strings);

		// Empty array
		assertThat(outputOf("min", ArrayType.of(List.of()))).isEqualTo(NullType.getInstance());
		assertThat(outputOf("max", ArrayType.of(List.of()))).isEqualTo(NullType.getInstance());

		// Open arrays
		assertThat(outputOf("min", ArrayType.of(numbers))).isEqualTo(UnionType.of(numbers, NullType.getInstance()));
		assertThat(outputOf("max", ArrayType.of(numbers))).isEqualTo(UnionType.of(numbers, NullType.getInstance()));
	}

	@Test
	void theKeyFilterOfSortBySeesTheElementNotTheArray() {
		// length accepts a string, so this is well typed only if the element type reaches the filter.
		assertThatThrownBy(() -> outputOf("sort_by(explode)", ArrayType.of(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void trimStrRejectsNonStringInputs() throws JsonQueryException {
		assertThat(outputOf("ltrimstr(\"a\")", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("rtrimstr(\"a\")", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> outputOf("ltrimstr(\"a\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("rtrimstr(\"a\")", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void pathBuiltinsAgreeOnTheShapeOfAPath() throws JsonQueryException {
		Type path = ArrayType.of(UnionType.of(StringType.getInstance(), NumericType.getInstance()));
		assertThat(outputOf("path(.a)", ObjectType.of(AnyType.getInstance()))).isEqualTo(path);
		assertThat(outputOf("[paths(type)]", ObjectType.of(AnyType.getInstance()))).isEqualTo(ArrayType.of(path));
		assertThat(outputOf("getpath([\"a\"])", ObjectType.of(AnyType.getInstance()))).isSameAs(AnyType.getInstance());
	}

	@Test
	void indexAndIndicesRejectObjectInputs() throws JsonQueryException {
		assertThat(outputOf("indices(\"a\")", ArrayType.of(AnyType.getInstance())))
				.isEqualTo(ArrayType.of(NumericType.of(NumberKind.INT)));
		assertThat(outputOf("index(\"a\")", ArrayType.of(AnyType.getInstance())))
				.isEqualTo(UnionType.of(NullType.getInstance(), NumericType.getInstance()));
		assertThat(outputOf("rindex(\"a\")", ArrayType.of(AnyType.getInstance())))
				.isEqualTo(UnionType.of(NullType.getInstance(), NumericType.getInstance()));

		assertThatThrownBy(() -> outputOf("indices(\"a\")", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("index(\"a\")", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("rindex(\"a\")", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void fromEntriesRequiresListOfEntryObjects() throws JsonQueryException {
		assertThat(outputOf("from_entries", Type.valueOf("[*:{key:STRING,value:ANY}]")))
				.isEqualTo(ObjectType.of(AnyType.getInstance()));
		assertThat(outputOf("from_entries", Type.valueOf("[*:{Key:STRING,Value:INT}]")))
				.isEqualTo(ObjectType.of(NumericType.of(NumberKind.INT)));
		assertThat(outputOf("from_entries", Type.valueOf("[*:{name:STRING,value:BOOLEAN}]")))
				.isEqualTo(ObjectType.of(BooleanType.getInstance()));
		assertThat(outputOf("from_entries", Type.valueOf("[*:{Name:STRING,Value:NULL}]")))
				.isEqualTo(ObjectType.of(NullType.getInstance()));

		assertThatThrownBy(() -> outputOf("from_entries", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("from_entries", ArrayType.of(NumericType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("from_entries", Type.valueOf("[*:{key:INT,value:ANY}]")))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void atBase64dRequiresStringInput() throws JsonQueryException {
		assertThat(outputOf("@base64d", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> outputOf("@base64d", NumericType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@base64d", BooleanType.getInstance()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@base64d", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void atShRequiresScalarOrArrayOfScalars() throws JsonQueryException {
		assertThat(outputOf("@sh", StringType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("@sh", NumericType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("@sh", BooleanType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("@sh", NullType.getInstance())).isSameAs(StringType.getInstance());
		assertThat(outputOf("@sh", ArrayType.of(StringType.getInstance()))).isSameAs(StringType.getInstance());
		assertThat(outputOf("@sh", ArrayType.of(NumericType.getInstance()))).isSameAs(StringType.getInstance());

		assertThatThrownBy(() -> outputOf("@sh", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@sh", ArrayType.of(ObjectType.of(AnyType.getInstance()))))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@sh", ArrayType.of(ArrayType.of(StringType.getInstance()))))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void atCsvAndTsvRequireArrayOfScalars() throws JsonQueryException {
		assertThat(outputOf("@csv", ArrayType.of(StringType.getInstance()))).isSameAs(StringType.getInstance());
		assertThat(outputOf("@tsv", ArrayType.of(NumericType.getInstance()))).isSameAs(StringType.getInstance());

		assertThatThrownBy(() -> outputOf("@csv", ArrayType.of(ObjectType.of(AnyType.getInstance()))))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@tsv", ArrayType.of(ArrayType.of(StringType.getInstance()))))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThatThrownBy(() -> outputOf("@csv", ObjectType.of(AnyType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}

	@Test
	void schemesWithNonMatchingArityAreIgnored() throws JsonQueryException {
		Function custom = new Function() {
			@Override
			public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
				return List.of(
						// Arity 0: non-matching arity, should be ignored by TypeCheck because call has 1 argument
						TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance())),
						// Arity 1: matching arity
						TypeScheme.of(FunctionType.of(StringType.getInstance(), NumericType.getInstance(), FilterType.of(AnyType.getInstance(), StringType.getInstance())))
				);
			}

			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(jsonProvider.createNumber(0), UntrackedPath.getInstance());
			}
		};
		FunctionLoader loader = new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return Map.of(FunctionSignature.of("custom", 1), custom);
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return Map.of();
			}
		};
		Environment<JsonNode> customEnv = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.addFunctionLoader(loader)
				.build();
		assertThat(customEnv.compile("custom(\"a\")", strict(StringType.getInstance())).getType().outputType())
				.isSameAs(NumericType.getInstance());

		Function mismatchOnly = new Function() {
			@Override
			public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
				return List.of(
						// Arity 0 only, called with 1 argument
						TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance()))
				);
			}

			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(jsonProvider.createNumber(0), UntrackedPath.getInstance());
			}
		};
		FunctionLoader mismatchLoader = new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return Map.of(FunctionSignature.of("mismatch", 1), mismatchOnly);
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return Map.of();
			}
		};
		Environment<JsonNode> mismatchEnv = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_7)
				.addFunctionLoader(mismatchLoader)
				.build();
		assertThatThrownBy(() -> mismatchEnv.compile("mismatch(\"a\")", strict(StringType.getInstance())))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
	}


	@Test
	void errorEndsTheFilter() throws JsonQueryException {
		assertThat(outputOf("error", AnyType.getInstance())).isSameAs(NeverType.getInstance());
		assertThat(outputOf("error(\"boom\")", AnyType.getInstance())).isSameAs(NeverType.getInstance());
	}

	@Test
	void inputConstrainedBuiltinsRejectTheWrongInput() {
		for (String query : List.of("explode", "keys", "to_entries", "@csv", "utf8bytelength", "sqrt")) {
			assertThatThrownBy(() -> outputOf(query, BooleanType.getInstance()))
					.describedAs(query)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("Type checking failed");
		}
	}

	@Test
	void joinAcceptsNumericElementsOnlyFromJq16() throws JsonQueryException {
		Type numbers = ArrayType.of(NumericType.getInstance());
		assertThat(environment(Versions.JQ_1_6).compile("join(\",\")", strict(numbers)).getType().outputType())
				.isSameAs(StringType.getInstance());
		assertThatThrownBy(() -> environment(Versions.JQ_1_5).compile("join(\",\")", strict(numbers)))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThat(environment(Versions.JQ_1_5).compile("join(\",\")", strict(ArrayType.of(StringType.getInstance())))
				.getType().outputType()).isSameAs(StringType.getInstance());
	}

	@Test
	void pathsFiltersTheRootOnlyFromJq171() throws JsonQueryException {
		// endswith needs a string, and from 1.7.1 the filter is applied to the root as well.
		ArrayList<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions warn = CompileOptions.newBuilder()
				.setTypeCheckMode(TypeCheckMode.WARN)
				.setInputType(ObjectType.of(AnyType.getInstance()))
				.setDiagnosticListener(diagnostics::add)
				.build();
		environment(Versions.JQ_1_7).compile("[paths(endswith(\"x\"))]", warn);
		assertThat(diagnostics).isEmpty();

		environment(Versions.JQ_1_7_1).compile("[paths(endswith(\"x\"))]", warn);
		assertThat(diagnostics).isNotEmpty();
	}
}
