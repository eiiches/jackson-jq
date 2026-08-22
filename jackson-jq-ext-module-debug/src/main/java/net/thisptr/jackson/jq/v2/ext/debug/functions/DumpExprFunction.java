package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class DumpExprFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		JsonNode dump = new Dumper<>(jsonProvider).dump(args.get(0));

		return new Expression<Context, JsonNode>() {
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
			public void apply(Context context, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				output.emit(dump, null);
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
			return serialize(expression);
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
				return jsonProvider.createNumber(((Long) value).longValue());
			if (value instanceof Float)
				return jsonProvider.createNumber(((Float) value).floatValue());
			if (value instanceof Double)
				return jsonProvider.createNumber(((Double) value).doubleValue());
			if (value instanceof Number)
				return jsonProvider.valueToTree(value);

			String previousIdentity = identities.get(value);
			if (previousIdentity != null)
				return reference(previousIdentity);
			String identity = identity(value);
			identities.put(value, identity);

			if (value instanceof Expression)
				return serializeExpression((Expression<?, ?>) value, identity);
			if (value.getClass().isArray())
				return serializeArray(value, identity);
			if (value instanceof Iterable)
				return serializeIterable((Iterable<?>) value, identity);
			if (value instanceof Map)
				return serializeMap((Map<?, ?>) value, identity);
			if (isStructural(value.getClass()))
				return serializeObject(value, identity);
			return serializeOpaque(value, identity);
		}

		private JsonNode serializeExpression(Expression<?, ?> expression, String identity) {
			@Var JsonNode node = objectEnvelope(expression, identity);
			node = jsonProvider.set(node, "cardinality", jsonProvider.createString(expression.getCardinality().name().toLowerCase(Locale.ROOT)));
			node = jsonProvider.set(node, "depends_on_input", jsonProvider.createBoolean(expression.dependsOnInput()));
			node = jsonProvider.set(node, "depends_on_external_state", jsonProvider.createBoolean(expression.dependsOnExternalState()));
			return serializeFields(expression, node);
		}

		private JsonNode serializeObject(Object value, String identity) {
			return serializeFields(value, objectEnvelope(value, identity));
		}

		private JsonNode serializeFields(Object value, @Var JsonNode node) {
			@Var JsonNode fieldsNode = jsonProvider.createObject();
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
				fieldsNode = jsonProvider.set(fieldsNode, name, fieldValue);
			}
			node = jsonProvider.set(node, "fields", fieldsNode);
			return node;
		}

		private JsonNode serializeArray(Object array, String identity) {
			@Var JsonNode elements = jsonProvider.createArray();
			for (int i = 0; i < Array.getLength(array); ++i)
				elements = jsonProvider.add(elements, serialize(Array.get(array, i)));
			@Var JsonNode node = objectEnvelope(array, identity);
			node = jsonProvider.set(node, "elements", elements);
			return node;
		}

		private JsonNode serializeIterable(Iterable<?> iterable, String identity) {
			@Var JsonNode elements = jsonProvider.createArray();
			for (Object element : iterable)
				elements = jsonProvider.add(elements, serialize(element));
			@Var JsonNode node = objectEnvelope(iterable, identity);
			node = jsonProvider.set(node, "elements", elements);
			return node;
		}

		private JsonNode serializeMap(Map<?, ?> map, String identity) {
			@Var JsonNode entries = jsonProvider.createArray();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				@Var JsonNode entryNode = jsonProvider.createObject();
				entryNode = jsonProvider.set(entryNode, "key", serialize(entry.getKey()));
				entryNode = jsonProvider.set(entryNode, "value", serialize(entry.getValue()));
				entries = jsonProvider.add(entries, entryNode);
			}
			@Var JsonNode node = objectEnvelope(map, identity);
			node = jsonProvider.set(node, "entries", entries);
			return node;
		}

		private JsonNode reference(String identity) {
			@Var JsonNode node = jsonProvider.createObject();
			node = jsonProvider.set(node, "$ref", jsonProvider.createString(identity));
			return node;
		}

		private JsonNode serializeOpaque(Object value, String identity) {
			@Var JsonNode node = objectEnvelope(value, identity);
			try {
				node = jsonProvider.set(node, "value", jsonProvider.createString(String.valueOf(value)));
			} catch (RuntimeException e) {
				node = jsonProvider.set(node, "stringification_failed", jsonProvider.createBoolean(true));
				node = jsonProvider.set(node, "error", jsonProvider.createString(e.getClass().getName()));
			}
			return node;
		}

		private JsonNode objectEnvelope(Object value, String identity) {
			@Var JsonNode node = jsonProvider.createObject();
			node = jsonProvider.set(node, "object", jsonProvider.createString(identity));
			node = jsonProvider.set(node, "class", jsonProvider.createString(value.getClass().getName()));
			return node;
		}

		private JsonNode inaccessible(Exception e) {
			@Var JsonNode node = jsonProvider.createObject();
			node = jsonProvider.set(node, "inaccessible", jsonProvider.createBoolean(true));
			node = jsonProvider.set(node, "error", jsonProvider.createString(e.getClass().getName()));
			if (e.getMessage() != null)
				node = jsonProvider.set(node, "message", jsonProvider.createString(e.getMessage()));
			return node;
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

		private static boolean isStructural(Class<?> type) {
			String name = type.getName();
			return name.startsWith("net.thisptr.jackson.jq.v2.core.internal.tree.")
					|| name.equals("net.thisptr.jackson.jq.v2.core.internal.misc.Pair")
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
