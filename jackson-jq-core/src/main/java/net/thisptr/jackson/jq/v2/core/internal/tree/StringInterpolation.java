package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StringInterpolation<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final List<Pair<Integer, Expression<StackFrame, JsonNode>>> interpolations;
	private final String template;
	private final @Nullable Expression<StackFrame, JsonNode> formatter;
	private final @Nullable Version version;

	@Override
	public Cardinality getCardinality() {
		Cardinality interpCard = CardinalityUtils.multiply(interpolations, p -> p._2.getCardinality());
		return formatter != null ? CardinalityUtils.multiply(formatter.getCardinality(), interpCard) : interpCard;
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public StringInterpolation(JsonProvider<JsonNode> jsonProvider, String template, List<Pair<Integer, Expression<StackFrame, JsonNode>>> interpolations, @Nullable Expression<StackFrame, JsonNode> formatter, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
		this.version = version;
		// formatter is already compiled under the correct shielded context (see Compiler's
		// StringInterpolationAstNode handling), so this is just a flat OR, same as everywhere else.
		List<Expression<StackFrame, JsonNode>> interpValues = new ArrayList<>(interpolations.size());
		for (Pair<Integer, Expression<StackFrame, JsonNode>> p : interpolations)
			interpValues.add(p._2);
		this.dependsOnInput = interpValues.stream().anyMatch(Expression::dependsOnInput)
				|| (formatter != null && formatter.dependsOnInput());
		this.dependsOnExternalState = interpValues.stream().anyMatch(Expression::dependsOnExternalState)
				|| (formatter != null && formatter.dependsOnExternalState());
		@Var Set<Integer> slots = FreeVariables.unionAll(interpValues);
		if (formatter != null && !FreeVariables.slotsOf(formatter).isEmpty()) {
			slots = new HashSet<>(slots);
			slots.addAll(FreeVariables.slotsOf(formatter));
		}
		this.freeLocalSlots = slots;
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(interpValues) || (formatter != null && FreeVariables.opaqueIn(formatter));
	}

	public String template() {
		return template;
	}

	public List<Pair<Integer, Expression<StackFrame, JsonNode>>> interpolations() {
		return interpolations;
	}

	public @Nullable Expression<StackFrame, JsonNode> formatter() {
		return formatter;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Deque<Pair<Integer, JsonNode>> stack = new ArrayDeque<>();
		recurse(frame, in, output, stack, interpolations);
	}

	private void recurse(StackFrame frame, JsonNode in, Output<JsonNode> output, Deque<Pair<Integer, JsonNode>> stack, List<Pair<Integer, Expression<StackFrame, JsonNode>>> interpolations) throws JsonQueryException {
		if (interpolations.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			@Var int pos = 0;
			for (Pair<Integer, JsonNode> head : stack) {
				builder.append(template.substring(pos, head._1));
				pos = head._1;

				JsonNodeType nodeType = jsonProvider.getNodeType(head._2);
				builder.append(nodeType == JsonNodeType.STRING ? jsonProvider.asText(head._2) : JsonNodeUtils.toString(jsonProvider, head._2, version));
			}
			builder.append(template.substring(pos));
			output.emit(jsonProvider.createString(builder.toString()), null);
		} else {
			Pair<Integer, Expression<StackFrame, JsonNode>> rhead = interpolations.get(interpolations.size() - 1);
			List<Pair<Integer, Expression<StackFrame, JsonNode>>> rtail = interpolations.subList(0, interpolations.size() - 1);
			rhead._2.apply(frame, in, null, (interpolated, opath) -> {
				if (formatter != null) {
					formatter.apply(frame, interpolated, null, (formatted, opath2) -> {
						stack.push(Pair.of(rhead._1, formatted));
						recurse(frame, in, output, stack, rtail);
						stack.pop();
					});
				} else {
					stack.push(Pair.of(rhead._1, interpolated));
					recurse(frame, in, output, stack, rtail);
					stack.pop();
				}
			});
		}
	}

	@Override
	public String toString() {
		@Var int pos = 0;
		StringBuilder builder = new StringBuilder();
		if (formatter != null) {
			builder.append(formatter);
			builder.append(" ");
		}
		builder.append("\"");
		for (Pair<Integer, Expression<StackFrame, JsonNode>> interpolation : interpolations) {
			copyEscaped(builder, template, pos, interpolation._1);
			pos = interpolation._1;
			builder.append("\\(");
			builder.append(interpolation._2);
			builder.append(")");
		}
		copyEscaped(builder, template, pos, template.length());
		builder.append("\"");
		return builder.toString();
	}

	private static void copyEscaped(StringBuilder builder, String text, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			char ch = text.charAt(i);
			switch (ch) {
				case '\\':
					builder.append("\\\\");
					break;
				case '"':
					builder.append("\\\"");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				case '\n':
					builder.append("\\n");
					break;
				default:
					builder.append(ch);
			}
		}
	}
}
