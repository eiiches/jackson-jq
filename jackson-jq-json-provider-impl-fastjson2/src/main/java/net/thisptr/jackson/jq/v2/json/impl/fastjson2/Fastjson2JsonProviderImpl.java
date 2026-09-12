package net.thisptr.jackson.jq.v2.json.impl.fastjson2;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * A JSON provider backed by Fastjson2's native object model.
 * <p>
 * Fastjson2 represents JSON {@code null} as Java {@code null}. The {@code Object} node type is
 * deliberately not annotated {@code @Nullable}: in this provider Java {@code null} is a JSON value,
 * not an absence signal.
 */
public class Fastjson2JsonProviderImpl implements JsonProvider<Object> {
	private static final Fastjson2JsonProviderImpl DEFAULT_INSTANCE = new Fastjson2JsonProviderImpl();

	/**
	 * Returns the shared default provider.
	 */
	public static Fastjson2JsonProviderImpl getInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public Object createArray(Iterable<?> values) {
		JSONArray result = new JSONArray();
		for (Object value : values)
			result.add(value);
		return result;
	}

	@Override
	public Object createObject(Map<String, ?> values) {
		JSONObject result = new JSONObject();
		for (Map.Entry<String, ?> entry : values.entrySet())
			result.put(entry.getKey(), entry.getValue());
		return result;
	}

	@Override
	public Object createString(String value) {
		return value;
	}

	@Override
	public Object createNumber(long value) {
		return value;
	}

	@Override
	public Object createNumber(int value) {
		return value;
	}

	@Override
	public Object createNumber(float value) {
		return value;
	}

	@Override
	public Object createNumber(double value) {
		return value;
	}

	@Override
	public Object createNumber(BigInteger value) {
		return value;
	}

	@Override
	public Object createNumber(BigDecimal value) {
		return value;
	}

	@Override
	public Object createBoolean(boolean value) {
		return value;
	}

	// Fastjson2 intentionally uses Java null as its JSON null node.
	@SuppressWarnings("NullAway")
	@Override
	public Object createNull() {
		return null;
	}

	@Override
	public Object createBinary(byte[] bytes) {
		throw new UnsupportedOperationException("Fastjson2 has no binary node type");
	}

	@Override
	public JsonNodeType getNodeType(Object node) {
		if (node == null)
			return JsonNodeType.NULL;
		if (node instanceof JSONObject)
			return JsonNodeType.OBJECT;
		if (node instanceof JSONArray)
			return JsonNodeType.ARRAY;
		if (node instanceof String)
			return JsonNodeType.STRING;
		if (node instanceof Number)
			return JsonNodeType.NUMBER;
		if (node instanceof Boolean)
			return JsonNodeType.BOOLEAN;
		throw new IllegalStateException("Unknown Fastjson2 node type: " + node.getClass());
	}

	@Override
	public boolean isObject(Object node) {
		return node instanceof JSONObject;
	}

	@Override
	public boolean isArray(Object node) {
		return node instanceof JSONArray;
	}

	@Override
	public boolean isString(Object node) {
		return node instanceof String;
	}

	@Override
	public boolean isNumber(Object node) {
		return node instanceof Number;
	}

	@Override
	public boolean isBoolean(Object node) {
		return node instanceof Boolean;
	}

	@Override
	public boolean isNull(Object node) {
		return node == null;
	}

	@Override
	public boolean isBinary(Object node) {
		return false;
	}

	@Override
	public NumberType getNumberType(Object node) {
		Number number = requireNumber(node, "number type");
		if (number instanceof Integer || number instanceof Short || number instanceof Byte)
			return NumberType.INT;
		if (number instanceof Long)
			return NumberType.LONG;
		if (number instanceof BigInteger)
			return NumberType.BIG_INTEGER;
		if (number instanceof BigDecimal)
			return NumberType.BIG_DECIMAL;
		if (number instanceof Double)
			return NumberType.DOUBLE;
		if (number instanceof Float)
			return NumberType.FLOAT;
		return NumberType.UNKNOWN;
	}

	@Override
	public boolean getBoolean(Object node) {
		if (!(node instanceof Boolean))
			throw new IllegalArgumentException("Cannot get the boolean value of " + getNodeType(node));
		return (Boolean) node;
	}

	@Override
	public double getNumberAsDoubleRounded(Object node) {
		return requireNumber(node, "double").doubleValue();
	}

	@Override
	public @Nullable BigDecimal getNumberAsBigDecimalExact(Object node) {
		return finiteDecimal(requireNumber(node, "BigDecimal"));
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerExact(Object node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "BigInteger"));
		if (value == null)
			return null;
		try {
			return value.toBigIntegerExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable BigInteger getNumberAsBigIntegerTruncated(Object node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "BigInteger"));
		return value == null ? null : value.toBigInteger();
	}

	@Override
	public String getString(Object node) {
		if (!(node instanceof String))
			throw new IllegalArgumentException("Cannot get the string value of " + getNodeType(node));
		return (String) node;
	}

	@Override
	public @Nullable Long getNumberAsLongExact(Object node) {
		Number number = requireNumber(node, "long");
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (!Double.isFinite(value) || value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63)
				return null;
			return (long) value;
		}
		BigDecimal value = finiteDecimal(number);
		if (value == null)
			return null;
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Long getNumberAsLongTruncated(Object node) {
		Number number = requireNumber(node, "long");
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (!Double.isFinite(value))
				return null;
			double truncated = value < 0 ? Math.ceil(value) : Math.floor(value);
			if (truncated < -0x1p63 || truncated >= 0x1p63)
				return null;
			return (long) truncated;
		}
		BigDecimal value = finiteDecimal(number);
		if (value == null)
			return null;
		try {
			return value.setScale(0, RoundingMode.DOWN).longValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntExact(Object node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "int"));
		if (value == null)
			return null;
		try {
			return value.intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public @Nullable Integer getNumberAsIntTruncated(Object node) {
		BigDecimal value = finiteDecimal(requireNumber(node, "int"));
		if (value == null)
			return null;
		try {
			return value.setScale(0, RoundingMode.DOWN).intValueExact();
		} catch (ArithmeticException e) {
			return null;
		}
	}

	private static Number requireNumber(Object node, String targetType) {
		if (!(node instanceof Number))
			throw new IllegalArgumentException("Cannot convert non-number to " + targetType);
		return (Number) node;
	}

	private static @Nullable BigDecimal finiteDecimal(Number number) {
		if ((number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue()))
			return null;
		if (number instanceof BigDecimal)
			return (BigDecimal) number;
		if (number instanceof BigInteger)
			return new BigDecimal((BigInteger) number);
		return new BigDecimal(number.toString());
	}

	@Override
	public byte[] getBinaryAsByteArray(Object node) {
		throw new IllegalArgumentException("Cannot get the binary value of " + getNodeType(node));
	}

	@Override
	public Iterator<Map.Entry<String, Object>> getObjectMembers(Object node) {
		return requireObject(node).entrySet().iterator();
	}

	@Override
	public Iterator<Object> getArrayElements(Object node) {
		return requireArray(node).iterator();
	}

	@Override
	public Iterator<Object> getObjectMemberValues(Object node) {
		return requireObject(node).values().iterator();
	}

	@Override
	public Iterator<String> getObjectMemberNames(Object node) {
		return requireObject(node).keySet().iterator();
	}

	@Override
	public Maybe<Object> getObjectMember(Object node, String name) {
		JSONObject object = requireObject(node);
		if (!object.containsKey(name))
			return Maybe.absent();
		return Maybe.of(object.get(name));
	}

	@Override
	public Object getObjectMemberOrDefault(Object node, String name, Object defaultValue) {
		JSONObject object = requireObject(node);
		return object.containsKey(name) ? object.get(name) : defaultValue;
	}

	@Override
	public Object getArrayElement(Object node, int index) {
		JSONArray array = requireArray(node);
		if (index < 0 || index >= array.size())
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for array length " + array.size());
		return array.get(index);
	}

	@Override
	public int getArrayLength(Object node) {
		return requireArray(node).size();
	}

	@Override
	public int getObjectMemberCount(Object node) {
		return requireObject(node).size();
	}

	@Override
	public boolean hasObjectMember(Object node, String name) {
		return requireObject(node).containsKey(name);
	}

	@Override
	public Object deepCopy(Object node) {
		if (node instanceof JSONObject) {
			JSONObject result = new JSONObject();
			for (Map.Entry<String, Object> entry : ((JSONObject) node).entrySet())
				result.put(entry.getKey(), deepCopy(entry.getValue()));
			return result;
		}
		if (node instanceof JSONArray) {
			JSONArray result = new JSONArray();
			for (Object value : (JSONArray) node)
				result.add(deepCopy(value));
			return result;
		}
		return node;
	}

	@Override
	public String format(Object node) {
		if (node == null)
			return "null";
		if (node instanceof Number) {
			Number number = (Number) node;
			if (number instanceof Double || number instanceof Float)
				return formatDouble(number.doubleValue());
			return number.toString();
		}
		if (node instanceof Boolean)
			return node.toString();
		if (node instanceof String)
			return JSON.toJSONString(node);
		if (node instanceof JSONArray) {
			StringBuilder result = new StringBuilder("[");
			@Var boolean first = true;
			for (Object value : (JSONArray) node) {
				if (!first)
					result.append(',');
				first = false;
				result.append(format(value));
			}
			return result.append(']').toString();
		}
		if (node instanceof JSONObject) {
			StringBuilder result = new StringBuilder("{");
			@Var boolean first = true;
			for (Map.Entry<String, Object> entry : ((JSONObject) node).entrySet()) {
				if (!first)
					result.append(',');
				first = false;
				result.append(JSON.toJSONString(entry.getKey()));
				result.append(':');
				result.append(format(entry.getValue()));
			}
			return result.append('}').toString();
		}
		throw new IllegalStateException("Unknown Fastjson2 node type: " + node.getClass());
	}

	private static String formatDouble(double value) {
		if (Double.isNaN(value))
			return "null";
		if (Double.isInfinite(value))
			return value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308";
		if (value == Math.floor(value) && Math.abs(value) < Long.MAX_VALUE)
			return String.valueOf((long) value);
		return normalizeExponent(String.valueOf(value));
	}

	private static String normalizeExponent(@Var String text) {
		text = text.replace('e', 'E');
		int exponent = text.indexOf('E');
		if (exponent >= 0 && exponent + 1 < text.length()) {
			char sign = text.charAt(exponent + 1);
			if (sign != '+' && sign != '-')
				text = text.substring(0, exponent + 1) + "+" + text.substring(exponent + 1);
		}
		return text;
	}

	@Override
	public JsonParser<Object> createParser(InputStream in) {
		return new Fastjson2Parser(in);
	}

	private static JSONObject requireObject(Object node) {
		if (!(node instanceof JSONObject))
			throw new IllegalArgumentException("Expected an object node");
		return (JSONObject) node;
	}

	private static JSONArray requireArray(Object node) {
		if (!(node instanceof JSONArray))
			throw new IllegalArgumentException("Expected an array node");
		return (JSONArray) node;
	}

	private static class Fastjson2Parser implements JsonParser<Object> {
		private final JSONReader reader;

		Fastjson2Parser(InputStream in) {
			JSONReader.Context context = JSONFactory.createReadContext(JSONReader.Feature.UseDoubleForDecimals);
			this.reader = JSONReader.of(in, StandardCharsets.UTF_8, context);
		}

		@Override
		public Maybe<Object> next() {
			try {
				if (reader.isEnd())
					return Maybe.absent();
				return Maybe.of(reader.readAny());
			} catch (JSONException e) {
				throw new JsonException(e);
			}
		}

		@Override
		public void close() {
			reader.close();
		}
	}
}
