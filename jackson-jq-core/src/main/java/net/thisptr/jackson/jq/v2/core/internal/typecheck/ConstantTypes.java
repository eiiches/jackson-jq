package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * The type of a value known at compile time, as {@code EnvironmentBuilder.defineConstant} registers it.
 * <p>
 * The rules match how {@code TypeCheck} types the equivalent jq literal, so {@code defineConstant("x", <1>)}
 * and {@code 1 as $x} describe {@code $x} the same way.
 * <p>
 * The walk is bounded: a value too deep or too large for the budget contributes {@link AnyType} instead of
 * a type of its own. Widening to {@code ANY} only costs checking -- it can never manufacture a diagnostic --
 * so a huge constant degrades quietly rather than building a type nothing can afford to carry around.
 */
public final class ConstantTypes {
	/**
	 * How many leading positions an array describes one by one, matching {@code TypeCheck}'s rule for an
	 * array literal. Past it the array is described by the union of its elements instead.
	 */
	private static final int MAX_KNOWN_ELEMENTS = 32;

	/**
	 * How many fields an object declares one by one. Past it the object is described as an open one whose
	 * fields have the union of its value types, since a type naming hundreds of fields is only expensive.
	 */
	private static final int MAX_KNOWN_FIELDS = 64;

	/**
	 * How deep the walk follows a value, and how many values it visits in total. A constant that a person
	 * writes sits far inside both; a parsed document need not.
	 */
	private static final int MAX_DEPTH = 16;
	private static final int MAX_NODES = 4096;

	private int remainingNodes = MAX_NODES;

	/**
	 * Whether a string or boolean is described by the value it holds, or only by being one.
	 */
	private final boolean knownValues;

	private ConstantTypes(boolean knownValues) {
		this.knownValues = knownValues;
	}

	/**
	 * Returns the type of {@code value}, describing the strings and booleans it holds by the very values
	 * they are -- which is what a constant is.
	 *
	 * @param jsonProvider the provider that reads {@code value}
	 * @param value the value to describe
	 * @return the type of {@code value}, never {@code null}
	 */
	public static <JsonNode> Type of(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		return new ConstantTypes(true).typeOf(jsonProvider, value, 0);
	}

	/**
	 * Returns the shape of {@code value}, describing a string as a string and a boolean as a boolean.
	 * <p>
	 * This is for a value standing in for others of its shape -- a sample document a type is read off --
	 * rather than for a constant. Pinning such a sample's strings would narrow a query against the one
	 * document it happened to be shown, and turn away the rest.
	 *
	 * @param jsonProvider the provider that reads {@code value}
	 * @param value the value whose shape to describe
	 * @return the shape of {@code value}, never {@code null}
	 */
	public static <JsonNode> Type shapeOf(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		return new ConstantTypes(false).typeOf(jsonProvider, value, 0);
	}

	private <JsonNode> Type typeOf(JsonProvider<JsonNode> jsonProvider, JsonNode value, int depth) {
		if (depth > MAX_DEPTH || remainingNodes <= 0)
			return AnyType.getInstance();
		--remainingNodes;
		return switch (jsonProvider.getNodeType(value)) {
			case NULL -> NullType.getInstance();
			case BOOLEAN -> knownValues ? BooleanType.of(jsonProvider.getBoolean(value)) : BooleanType.getInstance();
			case STRING -> knownValues ? StringType.of(jsonProvider.getString(value)) : StringType.getInstance();
			case BINARY -> BinaryType.getInstance();
			case NUMBER -> NumericType.of(numberKind(jsonProvider, value));
			case ARRAY -> arrayType(jsonProvider, value, depth);
			case OBJECT -> objectType(jsonProvider, value, depth);
		};
	}

	/**
	 * What is known about a number, from the value rather than from how the provider stores it -- the same
	 * test the compiler applies to a numeric literal. A value with no exact decimal form is a NaN or an
	 * infinity, which {@link NumberKind} counts as {@link NumberKind#FLOAT}.
	 */
	private static <JsonNode> NumberKind numberKind(JsonProvider<JsonNode> jsonProvider, JsonNode value) {
		BigDecimal decimal = jsonProvider.getNumberAsBigDecimalExact(value);
		if (decimal == null)
			return NumberKind.FLOAT;
		return decimal.stripTrailingZeros().scale() <= 0 ? NumberKind.INT : NumberKind.FLOAT;
	}

	private <JsonNode> Type arrayType(JsonProvider<JsonNode> jsonProvider, JsonNode value, int depth) {
		List<Type> elements = new ArrayList<>();
		Iterator<JsonNode> iterator = jsonProvider.getArrayElements(value);
		while (iterator.hasNext())
			elements.add(typeOf(jsonProvider, iterator.next(), depth + 1));
		return elements.size() <= MAX_KNOWN_ELEMENTS ? ArrayType.of(elements) : ArrayType.of(UnionType.of(elements));
	}

	/**
	 * A constant object really does have exactly the fields it has, so the type is closed and a query
	 * reading a field that is not there is told so.
	 */
	private <JsonNode> Type objectType(JsonProvider<JsonNode> jsonProvider, JsonNode value, int depth) {
		Map<String, Type> fields = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> iterator = jsonProvider.getObjectMembers(value);
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> member = iterator.next();
			fields.put(member.getKey(), typeOf(jsonProvider, member.getValue(), depth + 1));
		}
		return fields.size() <= MAX_KNOWN_FIELDS ? ObjectType.of(fields) : ObjectType.of(UnionType.of(fields.values()));
	}
}
