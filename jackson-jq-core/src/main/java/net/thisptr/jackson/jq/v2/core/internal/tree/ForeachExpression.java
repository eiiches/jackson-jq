package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ForeachExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	/**
	 * The first jq release whose {@code foreach} hands its source the real input for every init value.
	 * <p>
	 * Before it, jq read {@code .} off the stack with {@code DUPN} when it entered the loop, which left a
	 * null behind in the slot the value came from. {@code reduce} was never fixed and does it still.
	 */
	public static final Version SOURCE_KEEPS_INPUT_ACROSS_INIT_VALUES_SINCE = Version.of(1, 8, 0);

	private final JsonProvider<JsonNode> jsonProvider;
	private final AnalyzedExpression<JsonNode> iterExpr;
	private final AnalyzedExpression<JsonNode> updateExpr;
	private final AnalyzedExpression<JsonNode> initExpr;
	private final @Nullable AnalyzedExpression<JsonNode> extractExpr;
	private final PatternMatcher<JsonNode> matcher;
	// `extractExpr` needs no counter: when present it emits this foreach's own values.
	private final int initOutputIndex;
	private final int updateOutputIndex;
	private final int iterOutputIndex;
	private final Set<Integer> matcherSlots;
	private final Version version;
	private final boolean sourceSeesNullAfterFirstInitValue;

	@Override
	public Cardinality getCardinality() {
		return extractExpr != null
				? CardinalityUtils.multiply(initExpr.getCardinality(), iterExpr.getCardinality(), updateExpr.getCardinality(), extractExpr.getCardinality())
				: CardinalityUtils.multiply(initExpr.getCardinality(), iterExpr.getCardinality(), updateExpr.getCardinality());
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ForeachExpression(JsonProvider<JsonNode> jsonProvider, PatternMatcher<JsonNode> matcher, AnalyzedExpression<JsonNode> initExpr, AnalyzedExpression<JsonNode> updateExpr, @Nullable AnalyzedExpression<JsonNode> extractExpr, AnalyzedExpression<JsonNode> iterExpr, Version version, Set<Integer> matcherSlots, int initOutputIndex, int updateOutputIndex, int iterOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.matcher = matcher;
		this.initOutputIndex = initOutputIndex;
		this.updateOutputIndex = updateOutputIndex;
		this.iterOutputIndex = iterOutputIndex;
		this.matcherSlots = matcherSlots;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
		this.version = version;
		this.sourceSeesNullAfterFirstInitValue = version.compareTo(SOURCE_KEEPS_INPUT_ACROSS_INIT_VALUES_SINCE) < 0
				&& initExpr.getCardinality() == Cardinality.UNKNOWN;
		// updateExpr sees the accumulator rather than `.`, and extractExpr sees updateExpr's own output, so
		// both have their input dependency discharged by initExpr and iterExpr -- which between them
		// determine the accumulator. dependsOnExternalState stays a flat OR: rebinding `.` cannot make a
		// clock or a file read deterministic. The matcher's bound slot(s) are only "closed" for
		// updateExpr/extractExpr -- they're not yet bound while initExpr/iterExpr run.
		this.dependsOnInput = initExpr.dependsOnInput() || iterExpr.dependsOnInput();
		this.dependsOnExternalState = initExpr.dependsOnExternalState() || iterExpr.dependsOnExternalState() || updateExpr.dependsOnExternalState()
				|| (extractExpr != null && extractExpr.dependsOnExternalState());
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(initExpr, iterExpr, updateExpr, extractExpr);
		this.freeLocalSlots = FreeVariables.minus(
				FreeVariables.union(initExpr, iterExpr, updateExpr, extractExpr),
				new ArrayList<>(matcherSlots));
	}

	public AnalyzedExpression<JsonNode> iterExpr() {
		return iterExpr;
	}

	public AnalyzedExpression<JsonNode> initExpr() {
		return initExpr;
	}

	public AnalyzedExpression<JsonNode> updateExpr() {
		return updateExpr;
	}

	public @Nullable AnalyzedExpression<JsonNode> extractExpr() {
		return extractExpr;
	}

	public PatternMatcher<JsonNode> matcher() {
		return matcher;
	}

	/**
	 * Whether this foreach hands its source a null for every init value after the first, which jq did until
	 * {@link #SOURCE_KEEPS_INPUT_ACROSS_INIT_VALUES_SINCE} and only where the init can emit a second value.
	 */
	public boolean sourceSeesNullAfterFirstInitValue() {
		return sourceSeesNullAfterFirstInitValue;
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
		AnalyzedExpression<JsonNode> rewrittenIter = rewriter.rewrite(iterExpr);
		AnalyzedExpression<JsonNode> rewrittenInit = rewriter.rewrite(initExpr);
		PatternMatcher<JsonNode> rewrittenMatcher = matcher.rewriteExpressions(rewriter::rewrite);
		AnalyzedExpression<JsonNode> rewrittenUpdate = rewriter.rewrite(updateExpr);
		AnalyzedExpression<JsonNode> rewrittenExtract = extractExpr != null ? rewriter.rewrite(extractExpr) : null;
		return rewrittenIter == iterExpr && rewrittenInit == initExpr && rewrittenMatcher == matcher
				&& rewrittenUpdate == updateExpr && rewrittenExtract == extractExpr
				? this
				: new ForeachExpression<>(jsonProvider, rewrittenMatcher, rewrittenInit, rewrittenUpdate, rewrittenExtract, rewrittenIter, version, matcherSlots, initOutputIndex, updateOutputIndex, iterOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		// Before jq 1.8, foreach read `.` off the stack with DUPN when it entered the loop, which left a
		// null behind in the slot the value came from: iterExpr saw the real input only while the first
		// initExpr value ran, and null for every initExpr value after it. Under path tracking that null is
		// a value the path no longer leads to, so the path goes stale there and any step off it is an
		// invalid path expression, exactly as in jq. Wrap in array to allow mutation inside lambda.
		boolean @Nullable [] firstInitValue = sourceSeesNullAfterFirstInitValue ? new boolean[] { true } : null;
		initExpr.apply(frame, in, UntrackedPath.getInstance(), (accumulator, accumulatorPath) -> {
			memory.countOutput(initOutputIndex);
			boolean firstInit = firstInitValue == null || firstInitValue[0];
			if (firstInitValue != null)
				firstInitValue[0] = false;
			JsonNode iterInput = firstInit ? in : jsonProvider.createNull();
			Path<JsonNode> iterInputPath = firstInit ? ipath : PathUtils.stale(UntrackedPath.getInstance(), ipath, in);
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			// Only the source moves the traversal along: the accumulator is a value of its own, so
			// whatever the body makes of it is emitted from wherever the current item is. A body that
			// hands back the item unchanged therefore keeps the item's path, and anything else loses it.
			@SuppressWarnings("unchecked")
			Path<JsonNode>[] outputPaths = (Path<JsonNode>[]) new Path<?>[] { ipath };

			// The matcher binds its variables straight into the frame, so by the time onMatch runs
			// updateExpr can simply read them.
			PatternMatcher.OnMatch onMatch = () -> {
				updateExpr.apply(frame, accumulators[0], UntrackedPath.getInstance(), (newaccumulator, newaccumulatorPath) -> {
					memory.countOutput(updateOutputIndex);
					if (extractExpr != null) {
						extractExpr.apply(frame, newaccumulator, UntrackedPath.getInstance(), (extracted, extractedPath) -> output.emit(extracted, outputPaths[0]));
					} else {
						output.emit(newaccumulator, outputPaths[0]);
					}
					accumulators[0] = newaccumulator;
				});
			};
			iterExpr.apply(frame, iterInput, iterInputPath, (item, itemPath) -> {
				memory.countOutput(iterOutputIndex);
				outputPaths[0] = PathUtils.stale(UntrackedPath.getInstance(), itemPath, item);
				matcher.matchWithPath(frame, item, itemPath, onMatch);
			});
		});
	}
}
