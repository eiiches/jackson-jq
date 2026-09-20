package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class DumpExprFunction implements Function {
	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		JsonNode dump = new Dumper<>(jsonProvider).dump(args.get(0));
		return new Expression<>() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.ONE;
			}

			@Override
			public boolean dependsOnInput() {
				return false;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				output.emit(dump, UntrackedPath.getInstance());
			}
		};
	}

	private static final class Dumper<JsonNode> {
		private final JsonProvider<JsonNode> jsonProvider;
		private final IdentityHashMap<Object, String> identities = new IdentityHashMap<>();

		private Dumper(JsonProvider<JsonNode> jsonProvider) {
			this.jsonProvider = jsonProvider;
		}

		private JsonNode dump(Expression<?, JsonNode> expression) {
			return serialize(unwrap(expression));
		}

		/**
		 * Strips the instrumentation the compiler puts around a function argument, so that what is dumped is
		 * the expression the caller wrote.
		 * <p>
		 * Every argument handed to a function is wrapped so that what it emits can be charged against
		 * {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)} -- the compiler cannot know that
		 * {@code dump_expr} never evaluates its argument. The wrapper forwards every question asked of it, so
		 * it is invisible to the engine, but it would otherwise be the first thing reported here. Matched by
		 * name because this module sees core only through reflection.
		 */
		private static Object unwrap(Object value) {
			@Var Object current = value;
			while (current != null && TRANSPARENT_WRAPPERS.contains(current.getClass().getName())) {
				Object inner = fieldValue(current, "inner");
				if (inner == null)
					break;
				current = inner;
			}
			return current;
		}

		private static @Nullable Object fieldValue(Object owner, String name) {
			for (Field field : instanceFields(owner.getClass())) {
				if (!field.getName().equals(name))
					continue;
				try {
					field.setAccessible(true);
					return field.get(owner);
				} catch (ReflectiveOperationException | RuntimeException e) {
					return null;
				}
			}
			return null;
		}

		private JsonNode serialize(@Nullable Object value) {
			if (value == null)
				return jsonProvider.createNull();
			if (value instanceof String || value instanceof Character || value instanceof Enum)
				return jsonProvider.createString(value.toString());
			if (value instanceof Boolean)
				return jsonProvider.createBoolean((Boolean) value);
			if (value instanceof Byte || value instanceof Short || value instanceof Integer)
				return jsonProvider.createNumber(((Number) value).intValue());
			if (value instanceof Long)
				return jsonProvider.createNumber((Long) value);
			if (value instanceof Float)
				return jsonProvider.createNumber((Float) value);
			if (value instanceof Double)
				return jsonProvider.createNumber((Double) value);
			if (value instanceof BigInteger)
				return jsonProvider.createNumber((BigInteger) value);
			if (value instanceof BigDecimal)
				return jsonProvider.createNumber((BigDecimal) value);
			if (value instanceof Number)
				return jsonProvider.createNumber(((Number) value).doubleValue());
			String previousIdentity = identities.get(value);
			if (previousIdentity != null)
				return reference(previousIdentity);
			String identity = identity(value);
			identities.put(value, identity);
			if (value instanceof Expression<?, ?> expr)
				return serializeExpression(expr, identity);
			if (value.getClass().isArray())
				return serializeArray(value, identity);
			if (value instanceof Iterable<?> iterable)
				return serializeIterable(iterable, identity);
			if (value instanceof Map<?, ?> map)
				return serializeMap(map, identity);
			if (isStructural(value.getClass()))
				return serializeObject(value, identity);
			return serializeOpaque(value, identity);
		}

		private JsonNode serializeExpression(Expression<?, ?> expression, String identity) {
			Map<String, JsonNode> node = objectEnvelope(expression, identity);
			node.put("cardinality", jsonProvider.createString(expression.getCardinality().name().toLowerCase(Locale.ROOT)));
			node.put("depends_on_input", jsonProvider.createBoolean(expression.dependsOnInput()));
			node.put("depends_on_external_state", jsonProvider.createBoolean(expression.dependsOnExternalState()));
			putFields(expression, node);
			return jsonProvider.createObject(node);
		}

		private JsonNode serializeObject(Object value, String identity) {
			Map<String, JsonNode> node = objectEnvelope(value, identity);
			putFields(value, node);
			return jsonProvider.createObject(node);
		}

		private void putFields(Object value, Map<String, JsonNode> node) {
			Map<String, JsonNode> fields = new LinkedHashMap<>();
			for (Field field : instanceFields(value.getClass())) {
				String name = fieldName(field, value.getClass());
				@Var JsonNode fieldValue;
				try {
					if (!field.isAccessible())
						field.setAccessible(true);
					fieldValue = serialize(field.get(value));
				} catch (IllegalAccessException | RuntimeException e) {
					fieldValue = inaccessible(e);
				}
				fields.put(name, fieldValue);
			}
			node.put("fields", jsonProvider.createObject(fields));
		}

		private JsonNode serializeArray(Object array, String identity) {
			List<JsonNode> elements = new ArrayList<>(Array.getLength(array));
			for (int i = 0; i < Array.getLength(array); ++i)
				elements.add(serialize(Array.get(array, i)));
			Map<String, JsonNode> node = objectEnvelope(array, identity);
			node.put("elements", jsonProvider.createArray(elements));
			return jsonProvider.createObject(node);
		}

		private JsonNode serializeIterable(Iterable<?> iterable, String identity) {
			List<JsonNode> elements = new ArrayList<>();
			for (Object element : iterable)
				elements.add(serialize(element));
			Map<String, JsonNode> node = objectEnvelope(iterable, identity);
			node.put("elements", jsonProvider.createArray(elements));
			return jsonProvider.createObject(node);
		}

		private JsonNode serializeMap(Map<?, ?> map, String identity) {
			List<JsonNode> entries = new ArrayList<>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Map<String, JsonNode> entryNode = new LinkedHashMap<>();
				entryNode.put("key", serialize(entry.getKey()));
				entryNode.put("value", serialize(entry.getValue()));
				entries.add(jsonProvider.createObject(entryNode));
			}
			Map<String, JsonNode> node = objectEnvelope(map, identity);
			node.put("entries", jsonProvider.createArray(entries));
			return jsonProvider.createObject(node);
		}

		private JsonNode reference(String identity) {
			return jsonProvider.createObject(Collections.singletonMap("$ref", jsonProvider.createString(identity)));
		}

		private JsonNode serializeOpaque(Object value, String identity) {
			Map<String, JsonNode> node = objectEnvelope(value, identity);
			try {
				node.put("value", jsonProvider.createString(String.valueOf(value)));
			} catch (RuntimeException e) {
				node.put("stringification_failed", jsonProvider.createBoolean(true));
				node.put("error", jsonProvider.createString(e.getClass().getName()));
			}
			return jsonProvider.createObject(node);
		}

		private Map<String, JsonNode> objectEnvelope(Object value, String identity) {
			Map<String, JsonNode> node = new LinkedHashMap<>();
			node.put("object", jsonProvider.createString(identity));
			node.put("class", jsonProvider.createString(value.getClass().getName()));
			return node;
		}

		private JsonNode inaccessible(Exception e) {
			Map<String, JsonNode> node = new LinkedHashMap<>();
			node.put("inaccessible", jsonProvider.createBoolean(true));
			node.put("error", jsonProvider.createString(e.getClass().getName()));
			if (e.getMessage() != null)
				node.put("message", jsonProvider.createString(e.getMessage()));
			return jsonProvider.createObject(node);
		}

		private static List<Field> instanceFields(Class<?> type) {
			List<Field> fields = new ArrayList<>();
			for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					if (!Modifier.isStatic(field.getModifiers()))
						fields.add(field);
				}
			}
			fields.sort(Comparator.comparing(Field::getName).thenComparing(field -> field.getDeclaringClass().getName()));
			return fields;
		}

		private static final Set<String> TRANSPARENT_WRAPPERS = Set.of(
				"net.thisptr.jackson.jq.v2.core.internal.tree.MeteredOutputExpression",
				"net.thisptr.jackson.jq.v2.core.internal.tree.MeteredConstantOutputExpression");

		private static boolean isStructural(Class<?> type) {
			String name = type.getName();
			return name.startsWith("net.thisptr.jackson.jq.v2.core.internal.tree.")
					|| name.equals("net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair")
					|| name.equals("net.thisptr.jackson.jq.v2.core.internal.compile.BoundArgumentInfo")
					|| name.equals("net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec")
					|| name.startsWith("net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec$")
					|| name.equals("net.thisptr.jackson.jq.v2.core.internal.compile.JqFunctionCompiler$ResolvedFunction");
		}

		private static String identity(Object value) {
			return value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
		}

		private static String fieldName(Field field, Class<?> type) {
			@Var int count = 0;
			for (Field candidate : instanceFields(type)) {
				if (candidate.getName().equals(field.getName()))
					++count;
			}
			if (count == 1)
				return field.getName();
			return field.getDeclaringClass().getName() + "#" + field.getName();
		}
	}
}
