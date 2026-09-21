package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class StringInterpolation<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final List<Pair<Integer, AnalyzedExpression<JsonNode>>> interpolations;
	private final String template;
	private final @Nullable AnalyzedExpression<JsonNode> formatter;
	private final Version version;
	// One counter per interpolated expression, plus one for the @format applied to each.
	private final int[] interpolationOutputIndices;
	private final int formatterOutputIndex;

	@Override
	public Cardinality getCardinality() {
		Cardinality interpCard = CardinalityUtils.multiply(interpolations, p -> p._2.getCardinality());
		return formatter != null ? CardinalityUtils.multiply(formatter.getCardinality(), interpCard) : interpCard;
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public StringInterpolation(JsonProvider<JsonNode> jsonProvider, String template, List<Pair<Integer, AnalyzedExpression<JsonNode>>> interpolations, @Nullable AnalyzedExpression<JsonNode> formatter, Version version, int[] interpolationOutputIndices, int formatterOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.template = template;
		this.interpolations = interpolations;
		this.formatter = formatter;
		this.version = version;
		this.interpolationOutputIndices = interpolationOutputIndices;
		this.formatterOutputIndex = formatterOutputIndex;
		// formatter is already compiled under the correct shielded context (see Compiler's
		// StringInterpolationAstNode handling), so this is just a flat OR, same as everywhere else.
		List<AnalyzedExpression<JsonNode>> interpValues = new ArrayList<>(interpolations.size());
		for (Pair<Integer, AnalyzedExpression<JsonNode>> p : interpolations)
			interpValues.add(p._2);
		// The formatter sees each interpolated value rather than `.`, so its own input dependency is
		// discharged by the interpolation expressions'.
		this.dependsOnInput = interpValues.stream().anyMatch(AnalyzedExpression::dependsOnInput);
		this.dependsOnExternalState = interpValues.stream().anyMatch(AnalyzedExpression::dependsOnExternalState)
				|| (formatter != null && formatter.dependsOnExternalState());
		@Var Set<Integer> slots = FreeVariables.unionAll(interpValues);
		if (formatter != null && !FreeVariables.slotsOf(formatter).isEmpty()) {
			slots = new HashSet<>(slots);
			slots.addAll(FreeVariables.slotsOf(formatter));
		}
		this.freeLocalSlots = slots;
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(interpValues) || (formatter != null && FreeVariables.opaqueIn(formatter));
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
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		@Var List<Pair<Integer, AnalyzedExpression<JsonNode>>> rewrittenInterpolations = null;
		for (int i = 0; i < interpolations.size(); i++) {
			Pair<Integer, AnalyzedExpression<JsonNode>> interpolation = interpolations.get(i);
			AnalyzedExpression<JsonNode> replacement = rewriter.rewrite(interpolation._2);
			if (rewrittenInterpolations == null && replacement != interpolation._2)
				rewrittenInterpolations = new ArrayList<>(interpolations);
			if (rewrittenInterpolations != null)
				rewrittenInterpolations.set(i, Pair.of(interpolation._1, replacement));
		}
		AnalyzedExpression<JsonNode> rewrittenFormatter = formatter != null ? rewriter.rewrite(formatter) : null;
		return rewrittenInterpolations == null && rewrittenFormatter == formatter
				? this
				: new StringInterpolation<>(jsonProvider, template,
				rewrittenInterpolations != null ? rewrittenInterpolations : interpolations,
				rewrittenFormatter, version, interpolationOutputIndices, formatterOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Deque<Pair<Integer, JsonNode>> stack = new ArrayDeque<>();
		recurse(frame, in, output, stack, interpolations);
	}

	private void recurse(StackFrame frame, JsonNode in, Output<JsonNode> output, Deque<Pair<Integer, JsonNode>> stack, List<Pair<Integer, AnalyzedExpression<JsonNode>>> interpolations) throws JsonQueryException {
		if (interpolations.isEmpty()) {
			RuntimeLimits limits = frame.getRuntimeLimits();
			StringBuilder builder = new StringBuilder();
			@Var int pos = 0;
			for (Pair<Integer, JsonNode> head : stack) {
				append(limits, builder, template.substring(pos, head._1));
				pos = head._1;
				JsonNodeType nodeType = jsonProvider.getNodeType(head._2);
				append(limits, builder, nodeType == JsonNodeType.STRING ? jsonProvider.getString(head._2) : JsonNodeUtils.toString(jsonProvider, head._2, version));
			}
			append(limits, builder, template.substring(pos));
			output.emit(jsonProvider.createString(builder.toString()), UntrackedPath.getInstance());
		} else {
			Pair<Integer, AnalyzedExpression<JsonNode>> rhead = interpolations.get(interpolations.size() - 1);
			List<Pair<Integer, AnalyzedExpression<JsonNode>>> rtail = interpolations.subList(0, interpolations.size() - 1);
			Memory memory = frame.getEnclosingMemory();
			rhead._2.apply(frame, in, UntrackedPath.getInstance(), (interpolated, opath) -> {
				memory.countOutput(interpolationOutputIndices[interpolations.size() - 1]);
				if (formatter != null) {
					formatter.apply(frame, interpolated, UntrackedPath.getInstance(), (formatted, opath2) -> {
						memory.countOutput(formatterOutputIndex);
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

	private static void append(RuntimeLimits limits, StringBuilder builder, String piece) {
		RuntimeLimitChecks.checkStringLength(limits, (long) builder.length() + piece.length());
		builder.append(piece);
	}
}
