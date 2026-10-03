package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "contains", nargs = 1)
public class ContainsFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, BooleanType.getInstance(), FilterType.of(INPUT, INPUT))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}


	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (value, opath) -> {
				if (jsonProvider.getNodeType(in) != jsonProvider.getNodeType(value)
						|| (jsonProvider.isBoolean(in) && jsonProvider.getBoolean(in) != jsonProvider.getBoolean(value))) {
					throw new JsonQueryTypeException("%s and %s cannot have their containment checked", ExceptionMessages.describe(jsonProvider, version, in), ExceptionMessages.describe(jsonProvider, version, value));
				}
				output.emit(jsonProvider.createBoolean(contains(jsonProvider, value, in)), UntrackedPath.getInstance());
			});
		};
	}

	private static <JsonNode> boolean contains(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) {
		Deque<ContainmentFrame<JsonNode>> stack = new ArrayDeque<>();
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		stack.push(new ContainmentFrame<>(needle, haystack));
		@Var boolean result = false;
		while (!stack.isEmpty()) {
			ContainmentFrame<JsonNode> current = stack.peek();
			switch (current.state) {
				case START -> {
					JsonNodeType hType = jsonProvider.getNodeType(current.haystack);
					JsonNodeType nType = jsonProvider.getNodeType(current.needle);
					if (hType == JsonNodeType.ARRAY && nType == JsonNodeType.ARRAY) {
						current.needleElements = jsonProvider.getArrayElements(current.needle);
						current.state = State.ARRAY_NEEDLE;
					} else if (hType == JsonNodeType.OBJECT && nType == JsonNodeType.OBJECT) {
						current.objectMembers = jsonProvider.getObjectMembers(current.needle);
						current.state = State.OBJECT_FIELD;
					} else {
						result = (hType == JsonNodeType.STRING && nType == JsonNodeType.STRING)
								? jsonProvider.getString(current.haystack).contains(jsonProvider.getString(current.needle))
								: comparator.compare(current.haystack, current.needle) == 0;
						stack.pop();
					}
				}
				case ARRAY_NEEDLE -> {
					if (!current.needleElements.hasNext()) {
						result = true;
						stack.pop();
					} else {
						current.currentNeedle = current.needleElements.next();
						current.haystackElements = jsonProvider.getArrayElements(current.haystack);
						current.state = State.ARRAY_HAYSTACK;
					}
				}
				case ARRAY_HAYSTACK -> {
					if (!current.haystackElements.hasNext()) {
						result = false;
						stack.pop();
					} else {
						current.state = State.ARRAY_AFTER_CHILD;
						stack.push(new ContainmentFrame<>(current.currentNeedle, current.haystackElements.next()));
					}
				}
				case ARRAY_AFTER_CHILD -> current.state = result ? State.ARRAY_NEEDLE : State.ARRAY_HAYSTACK;
				case OBJECT_FIELD -> {
					if (!current.objectMembers.hasNext()) {
						result = true;
						stack.pop();
					} else {
						Map.Entry<String, JsonNode> field = current.objectMembers.next();
						Maybe<JsonNode> value = jsonProvider.getObjectMember(current.haystack, field.getKey());
						if (value.isAbsent()) {
							result = false;
							stack.pop();
						} else {
							current.state = State.OBJECT_AFTER_CHILD;
							stack.push(new ContainmentFrame<>(field.getValue(), value.get()));
						}
					}
				}
				case OBJECT_AFTER_CHILD -> {
					if (result)
						current.state = State.OBJECT_FIELD;
					else
						stack.pop();
				}
			}
		}
		return result;
	}

	private enum State {
		START,
		ARRAY_NEEDLE,
		ARRAY_HAYSTACK,
		ARRAY_AFTER_CHILD,
		OBJECT_FIELD,
		OBJECT_AFTER_CHILD
	}

	private static final class ContainmentFrame<JsonNode> {
		private final JsonNode needle;
		private final JsonNode haystack;
		private State state = State.START;
		private Iterator<JsonNode> needleElements = Collections.emptyIterator();
		private Iterator<JsonNode> haystackElements = Collections.emptyIterator();
		private Iterator<Map.Entry<String, JsonNode>> objectMembers = Collections.emptyIterator();
		private JsonNode currentNeedle;

		private ContainmentFrame(JsonNode needle, JsonNode haystack) {
			this.needle = needle;
			this.haystack = haystack;
			this.currentNeedle = needle;
		}
	}
}
