package net.thisptr.jackson.jq.v2.core.internal.compile.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.ConstantFoldingOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the compiler evaluates ahead of time, and what it refuses to.
 * <p>
 * Folding is observable in two ways, and both are used here: a constant expression stops being evaluated
 * when the query runs, which {@code tick} counts; and the values it produced are visible to
 * {@code Function.bind} through {@link ConstantExpression}, which {@code probe} captures.
 */
public class ConstantFoldingTest {
	private static final JsonProvider<JsonNode> PROVIDER = Jackson2JsonProvider.getInstance();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private abstract static class AbstractPureExpression implements Expression<StackFrame, JsonNode>, FreeVariables {
		@Override
		public boolean dependsOnInput() {
			return false;
		}

		@Override
		public boolean dependsOnExternalState() {
			return false;
		}

		@Override
		public Set<Integer> freeLocalSlots() {
			return Collections.emptySet();
		}

		@Override
		public boolean hasOpaqueVariableReference() {
			return false;
		}
	}

	private static final class PlannerExpression extends AbstractPureExpression implements RewritableExpression<JsonNode> {
		private final String name;
		private final List<String> evaluations;
		private final @Nullable Expression<StackFrame, JsonNode> child;
		private final boolean fail;

		PlannerExpression(String name, List<String> evaluations, @Nullable Expression<StackFrame, JsonNode> child, boolean fail) {
			this.name = name;
			this.evaluations = evaluations;
			this.child = child;
			this.fail = fail;
		}

		@Nullable Expression<StackFrame, JsonNode> child() {
			return child;
		}

		@Override
		public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
			if (child == null)
				return this;
			Expression<StackFrame, JsonNode> rewritten = rewriter.rewrite(child);
			return rewritten == child ? this : new PlannerExpression(name, evaluations, rewritten, fail);
		}

		@Override
		public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) {
			evaluations.add(name);
			if (fail)
				throw new IllegalStateException("refuse this fold");
			output.emit(PROVIDER.createNull(), UntrackedPath.getInstance());
		}
	}

	/**
	 * Counts how often it is evaluated while claiming to be pure, so that a test can tell compile-time
	 * evaluations from run-time ones. A real function must not lie like this -- see
	 * {@code docs/optimizations.md} on what under-declaring {@code dependsOnExternalState()} costs.
	 */
	private static final class Tick implements Function {
		final AtomicInteger evaluations = new AtomicInteger();
		private final boolean usesInput;

		Tick() {
			this(false);
		}

		Tick(boolean usesInput) {
			this.usesInput = usesInput;
		}

		@Override
		public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
			JsonProvider<N> provider = ctx.getJsonProvider();
			return FunctionBody.<Context, N>builder(args).usesInput(usesInput).build((frame, in, path, output) -> {
				evaluations.incrementAndGet();
				output.emit(usesInput ? in : provider.createNumber(1), UntrackedPath.getInstance());
			});
		}
	}

	/**
	 * Captures the argument expression each call site hands it, at bind time.
	 */
	private static final class Probe implements Function {
		final List<Expression<?, ?>> arguments = new ArrayList<>();

		@Override
		public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
			arguments.add(args.get(0));
			return (frame, in, path, output) -> args.get(0).apply(frame, in, UntrackedPath.getInstance(), output);
		}
	}

	private static FunctionLoader loader(Map<FunctionSignature, Function> functions) {
		return new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return functions;
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return Collections.emptyMap();
			}
		};
	}

	private static Environment<JsonNode> env(Version version, Map<FunctionSignature, Function> functions) {
		return EnvironmentBuilder.withDefaultLoaders(PROVIDER, version).addFunctionLoader(loader(functions)).build();
	}

	private static CompileOptions folding(ConstantFoldingOptions constantFoldingOptions) {
		return CompileOptions.newBuilder().setConstantFoldingOptions(constantFoldingOptions).build();
	}

	private static List<JsonNode> apply(JsonQuery<JsonNode> query, String input) throws JsonQueryException {
		List<JsonNode> out = new ArrayList<>();
		try {
			query.apply(MAPPER.readTree(input), out::add);
		} catch (JsonQueryException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return out;
	}

	/**
	 * Compiles {@code query} in argument position and returns what it folded to, or {@code null} if it did
	 * not fold. An argument is finalized before {@code Function.bind} sees it, so this is the one place a
	 * test can read a fold's outcome directly.
	 */
	private static @Nullable List<String> foldedOf(Environment<JsonNode> env, Probe probe, String query) throws JsonQueryException {
		env.compile("probe(" + query + ")");
		List<JsonNode> folded = foldedValues(probe.arguments.get(probe.arguments.size() - 1));
		if (folded == null)
			return null;
		List<String> rendered = new ArrayList<>(folded.size());
		for (JsonNode value : folded)
			rendered.add(value.toString());
		return rendered;
	}

	private static @Nullable List<JsonNode> foldedValues(Expression<?, ?> expression) {
		if (!(expression instanceof ConstantExpression<?, ?>))
			return null;
		@SuppressWarnings("unchecked")
		ConstantExpression<?, JsonNode> constant = (ConstantExpression<?, JsonNode>) expression;
		return constant.getConstantResults();
	}

	// --- folding replaces evaluation ------------------------------------------------------------

	@Test
	public void aConstantExpressionIsEvaluatedOnceAtCompileTimeAndNeverAgain() throws Exception {
		Tick tick = new Tick();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("tick", 0), tick));

		JsonQuery<JsonNode> query = env.compile("[tick, tick + 1]");
		assertThat(tick.evaluations.get()).as("evaluated while compiling").isEqualTo(2);

		for (int i = 0; i < 5; i++)
			assertThat(apply(query, "null")).extracting(Object::toString).containsExactly("[1,2]");
		assertThat(tick.evaluations.get()).as("not evaluated again, however often the query runs").isEqualTo(2);
	}

	@Test
	public void anInputDependentExpressionIsEvaluatedEveryTime() throws Exception {
		Tick echo = new Tick(/* usesInput */ true);
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("echo", 0), echo));

		JsonQuery<JsonNode> query = env.compile("[.[] | echo]");
		assertThat(echo.evaluations.get()).as("nothing to fold, so nothing runs while compiling").isZero();
		assertThat(apply(query, "[1, 2, 3]")).extracting(Object::toString).containsExactly("[1,2,3]");
		assertThat(echo.evaluations.get()).isEqualTo(3);
	}

	// --- where folding reaches ------------------------------------------------------------------

	@Test
	public void thePlannerAttemptsAParentBeforeItsChildren() {
		List<String> evaluations = new ArrayList<>();
		FoldPlanner planner = new FoldPlanner(ConstantFoldingOptions.newBuilder().build());
		planner.beginRegion();
		int parentMark = planner.beginNode();
		int childMark = planner.beginNode();
		Expression<StackFrame, JsonNode> child = planner.endNode(childMark, new PlannerExpression("child", evaluations, null, false), 0, 0, 0);
		PlannerExpression parent = new PlannerExpression("parent", evaluations, child, true);
		planner.endNode(parentMark, parent, 0, 0, 0);

		Expression<StackFrame, JsonNode> optimized = planner.finishRegion(env(Versions.JQ_1_8_2, Collections.emptyMap()), parent);

		assertThat(evaluations).containsExactly("parent", "child");
		assertThat(optimized).isNotSameAs(parent).isInstanceOf(PlannerExpression.class);
		assertThat(((PlannerExpression) optimized).child()).isInstanceOf(ConstantExpression.class);
	}

	@Test
	public void aSuccessfulParentFoldSkipsItsChildren() {
		List<String> evaluations = new ArrayList<>();
		FoldPlanner planner = new FoldPlanner(ConstantFoldingOptions.newBuilder().build());
		planner.beginRegion();
		int parentMark = planner.beginNode();
		int childMark = planner.beginNode();
		Expression<StackFrame, JsonNode> child = planner.endNode(childMark, new PlannerExpression("child", evaluations, null, false), 0, 0, 0);
		PlannerExpression parent = new PlannerExpression("parent", evaluations, child, false);
		planner.endNode(parentMark, parent, 0, 0, 0);

		Expression<StackFrame, JsonNode> optimized = planner.finishRegion(env(Versions.JQ_1_8_2, Collections.emptyMap()), parent);

		assertThat(evaluations).containsExactly("parent");
		assertThat(optimized).isInstanceOf(ConstantExpression.class);
	}

	@Test
	public void foldingIsNotLimitedToFunctionArguments() throws Exception {
		Tick tick = new Tick();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("tick", 0), tick));

		// One compile-time evaluation per `tick` written, and none at all afterwards: every position here
		// folds, so nothing is left to evaluate. The positions are the pipe, array and object construction,
		// string interpolation, a reduce source, and an `as` binding.
		JsonQuery<JsonNode> query = env.compile(
				"[1 | tick, [tick], {a: tick}, \"\\(tick)\", reduce tick as $x (0; . + $x), (tick as $y | $y)]");
		int atCompileTime = tick.evaluations.get();
		assertThat(atCompileTime).isEqualTo(6);

		assertThat(apply(query, "null")).extracting(Object::toString).containsExactly("[1,[1],{\"a\":1},\"1\",1,1]");
		assertThat(tick.evaluations.get()).isEqualTo(atCompileTime);
	}

	@Test
	public void aConstantChildIsRewrittenInsideAnInputDependentParent() throws Exception {
		Tick tick = new Tick();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("tick", 0), tick));

		JsonQuery<JsonNode> query = env.compile("{constant: tick, input: .}");
		assertThat(tick.evaluations.get()).as("the constant field folded while compiling").isEqualTo(1);

		assertThat(apply(query, "2")).extracting(Object::toString).containsExactly("{\"constant\":1,\"input\":2}");
		assertThat(apply(query, "3")).extracting(Object::toString).containsExactly("{\"constant\":1,\"input\":3}");
		assertThat(tick.evaluations.get()).as("the reconstructed parent contains the folded child").isEqualTo(1);
	}

	@Test
	public void aFoldedArgumentStillReachesBindAsAConstantExpression() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		env.compile("probe(1, 2, 3)");
		assertThat(foldedValues(probe.arguments.get(0))).extracting(Object::toString).containsExactly("1", "2", "3");

		// Order is the emission order, and metering the argument does not hide its constancy.
		env.compile("probe(\"a\" + \"b\")");
		assertThat(foldedValues(probe.arguments.get(1))).extracting(Object::toString).containsExactly("\"ab\"");

		env.compile("probe(.)");
		assertThat(foldedValues(probe.arguments.get(2))).isNull();
	}

	// --- constructs that rebind `.` ---------------------------------------------------------------

	/**
	 * A construct that hands its child a value it computed itself -- a pipe's right side, a {@code catch},
	 * a {@code reduce}/{@code foreach} update, an assignment's base {@code .} -- reads the caller's input
	 * only as often as the expressions it computed that value from do. Each such node discharges its
	 * child's input dependency in its own {@code dependsOnInput()}, which is what lets the folder reach
	 * past them.
	 */
	@Test
	public void aConstructThatRebindsItsChildInputIsFoldedWhenWhatItRebindsFromIs() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		assertThat(foldedOf(env, probe, "1 | . + 1")).containsExactly("2");
		assertThat(foldedOf(env, probe, "reduce range(0; 3) as $x (0; . + $x)")).containsExactly("3");
		assertThat(foldedOf(env, probe, "foreach range(0; 3) as $x (0; . + $x)")).containsExactly("0", "1", "3");
		assertThat(foldedOf(env, probe, "try error(\"x\") catch .")).containsExactly("\"x\"");
		assertThat(foldedOf(env, probe, "{} | .a = 1")).containsExactly("{\"a\":1}");
		assertThat(foldedOf(env, probe, "\"\\(1 + 2)\"")).containsExactly("\"3\"");
	}

	@Test
	public void aConstructThatRebindsItsChildInputStillDependsOnWhatItRebindsFrom() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		// Each of these rebinds `.` for its child from something that is itself input-dependent, so the
		// dependency reaches the whole expression and nothing folds.
		assertThat(foldedOf(env, probe, ". | 1 + 2")).isNull();
		assertThat(foldedOf(env, probe, "reduce .[] as $x (0; . + $x)")).isNull();
		assertThat(foldedOf(env, probe, "foreach .[] as $x (0; . + $x)")).isNull();
		assertThat(foldedOf(env, probe, "try . catch \"y\"")).isNull();
		assertThat(foldedOf(env, probe, ".a = 1")).isNull();
		assertThat(foldedOf(env, probe, "\"\\(.)\"")).isNull();
	}

	@Test
	public void aConstantChildOfAnInputDependentPipeStillFolds() throws Exception {
		Tick tick = new Tick();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("tick", 0), tick));

		// The pipe itself reads `.`, so it is not folded; its right side does not, so it is.
		JsonQuery<JsonNode> query = env.compile(". | 1 + tick");
		assertThat(tick.evaluations.get()).isEqualTo(1);

		assertThat(apply(query, "null")).extracting(Object::toString).containsExactly("2");
		assertThat(tick.evaluations.get()).isEqualTo(1);
	}

	// --- the budget ------------------------------------------------------------------------------

	@Test
	public void aFoldIsAbandonedPastTheValueBudget() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		env.compile("probe([range(0; 256)] | .[])");
		assertThat(foldedValues(probe.arguments.get(0))).as("256 values fit").hasSize(256);

		env.compile("probe([range(0; 257)] | .[])");
		assertThat(foldedValues(probe.arguments.get(1))).as("257 do not").isNull();

		// Abandoning changes nothing but the compiled tree: the query still evaluates.
		assertThat(apply(env.compile("[range(0; 257)] | length"), "null")).extracting(Object::toString).containsExactly("257");
	}

	@Test
	public void aNonTerminatingConstantExpressionDoesNotHangCompilation() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());
		long started = System.nanoTime();
		assertThatCode(() -> env.compile("[last(range(0; 1e9)), until(false; 1)]")).doesNotThrowAnyException();
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	// --- error timing ----------------------------------------------------------------------------

	@Test
	public void aConstantJqErrorIsFoldedAndReplayedAtEvaluationTime() throws Exception {
		AtomicInteger evaluations = new AtomicInteger();
		Function fail = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
				return FunctionBody.<Context, N>builder(args).build((frame, in, path, output) -> {
					evaluations.incrementAndGet();
					throw new JsonQueryException("boom");
				});
			}
		};
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("fail", 0), fail));

		JsonQuery<JsonNode> query = env.compile("fail");
		assertThat(evaluations).hasValue(1);

		for (int i = 0; i < 3; i++)
			assertThatThrownBy(() -> apply(query, "null")).isInstanceOf(JsonQueryException.class).hasMessage("boom");
		assertThat(evaluations).as("the folded error is replayed without evaluating the function again").hasValue(1);
	}

	@Test
	public void valuesBeforeAConstantErrorAreFoldedAndReplayedInOrder() throws Exception {
		AtomicInteger evaluations = new AtomicInteger();
		Function fail = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
				return FunctionBody.<Context, N>builder(args).build((frame, in, path, output) -> {
					evaluations.incrementAndGet();
					throw new JsonQueryException("boom");
				});
			}
		};
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("fail", 0), fail));
		JsonQuery<JsonNode> query = env.compile("1, 2, fail");
		assertThat(evaluations).hasValue(1);

		List<JsonNode> output = new ArrayList<>();
		assertThatThrownBy(() -> query.apply(MAPPER.readTree("null"), output::add))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("boom");
		assertThat(output).extracting(Object::toString).containsExactly("1", "2");
		assertThat(evaluations).hasValue(1);
	}

	@Test
	public void aDownstreamErrorStopsReplayBeforeTheStoredError() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());
		JsonQuery<JsonNode> query = env.compile("1, error(\"stored\")");

		assertThatThrownBy(() -> query.apply(MAPPER.readTree("null"), value -> {
			throw new JsonQueryException("downstream");
		})).isInstanceOf(JsonQueryException.class).hasMessage("downstream");
	}

	@Test
	public void anErrorEndingArgumentIsNotExposedAsAConstantExpression() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		JsonQuery<JsonNode> query = env.compile("probe(error(\"boom\"))");
		assertThat(foldedValues(probe.arguments.get(0))).isNull();
		assertThatThrownBy(() -> apply(query, "null")).isInstanceOf(JsonQueryException.class).hasMessageContaining("boom");

		// An error the expression itself catches is not a failure, so that folds.
		env.compile("probe(try error(\"boom\") catch .)");
		assertThat(foldedValues(probe.arguments.get(1))).extracting(Object::toString).containsExactly("\"boom\"");
	}

	@Test
	public void runtimeLimitErrorsAreNotFolded() throws Exception {
		AtomicInteger evaluations = new AtomicInteger();
		Function limit = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
				return FunctionBody.<Context, N>builder(args).build((frame, in, path, output) -> {
					evaluations.incrementAndGet();
					throw new RuntimeLimitExceededException("limit");
				});
			}
		};
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("limit_now", 0), limit));
		JsonQuery<JsonNode> query = env.compile("limit_now");
		int afterCompile = evaluations.get();

		assertThatThrownBy(() -> apply(query, "null")).isInstanceOf(RuntimeLimitExceededException.class).hasMessage("limit");
		assertThat(evaluations).hasValue(afterCompile + 1);
	}

	@Test
	public void foldedBreakRetainsItsControlFlowSubtype() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		assertThat(apply(env.compile("label $out | probe(1, break $out, 2)"), "null"))
				.extracting(Object::toString)
				.containsExactly("1");
		assertThat(foldedValues(probe.arguments.get(0))).isNull();
	}

	@Test
	public void aFunctionThatThrowsSomethingOtherThanJsonQueryExceptionDoesNotBreakCompilation() throws Exception {
		Function explode = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> ctx, List<Expression<Context, N>> args) {
				return FunctionBody.<Context, N>builder(args).build((frame, in, path, output) -> {
					throw new IllegalStateException("not a JsonQueryException");
				});
			}
		};
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("explode_now", 0), explode));

		JsonQuery<JsonNode> query = env.compile("[explode_now]");
		assertThatThrownBy(() -> apply(query, "null")).isInstanceOf(JsonQueryException.class);
	}

	// --- barriers ---------------------------------------------------------------------------------

	@Test
	public void aSubtreeThatInstallsADefIsNotFolded() throws Exception {
		Probe probe = new Probe();
		Map<FunctionSignature, Function> functions = Collections.singletonMap(FunctionSignature.of("probe", 1), probe);
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, functions);

		// A def's Function goes into the enclosing frame, and the call that reads it can sit outside the
		// expression that put it there -- folding would evaluate the install and discard it, leaving the call
		// an empty slot. The def's own body still folds; it is the subtree around it that cannot.
		env.compile("probe(def f: 1; f)");
		assertThat(foldedValues(probe.arguments.get(0))).as("a def in the subtree blocks the fold").isNull();

		env.compile("probe(1)");
		assertThat(foldedValues(probe.arguments.get(1))).as("control: the same shape without a def").isNotNull();

		// And the shapes that would break if the barrier were missing still evaluate correctly.
		assertThat(apply(env.compile("def f: 1 + 1; 1 as $_ | f"), "null")).extracting(Object::toString).containsExactly("2");
		assertThat(apply(env.compile("def f: 1 + 1; f"), "null")).extracting(Object::toString).containsExactly("2");
	}

	@Test
	public void aTryIsNotFoldedBeforeJq17BecauseItCatchesWhatItsConsumerThrows() throws Exception {
		Probe legacy = new Probe();
		env(Versions.JQ_1_6, Collections.singletonMap(FunctionSignature.of("probe", 1), legacy)).compile("probe(try 1 catch .)");
		assertThat(foldedValues(legacy.arguments.get(0)))
				.as("a 1.6 try also catches what its consumer throws, so it is not a function of its own subtree")
				.isNull();

		Probe modern = new Probe();
		env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), modern)).compile("probe(try 1 catch .)");
		assertThat(foldedValues(modern.arguments.get(0)))
				.as("from 1.7 a downstream error escapes the try, so it folds like anything else")
				.extracting(Object::toString)
				.containsExactly("1");
	}

	// --- path mode --------------------------------------------------------------------------------

	@Test
	public void aFoldedExpressionStepsAsideWhenAPathIsBeingTracked() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());

		// Evaluated for its value `[] | .c?` yields nothing, which is what the folder collects. Asked for a
		// path it is an error, because `?` does not make an invalid path valid -- so the folded values are no
		// answer here and the expression has to run.
		assertThat(apply(env.compile("try path([] | .c?) catch ."), "null"))
				.extracting(Object::toString)
				.containsExactly("\"Invalid path expression near attempt to access element \\\"c\\\" of []\"");

		// A constant is not a path expression, and says so with the folded value in the message.
		assertThat(apply(env.compile("try path(1 + 1) catch ."), "null"))
				.extracting(Object::toString)
				.containsExactly("\"Invalid path expression with result 2\"");
	}

	// --- the budget is the caller's ---------------------------------------------------------------

	@Test
	public void raisingMaxResultsFoldsWhatTheDefaultRefuses() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		// A bare generator, so maxResults is the only cap in play -- wrapping it in an array would hit
		// maxArrayLength first, which is what theEmbeddedRuntimeOptionsBoundWhatAFoldMayBuild covers.
		env.compile("probe(range(0; 257))");
		assertThat(foldedValues(probe.arguments.get(0))).as("257 is past the default of 256").isNull();

		env.compile("probe(range(0; 257))",
				folding(ConstantFoldingOptions.newBuilder().setMaxResults(300).build()));
		assertThat(foldedValues(probe.arguments.get(1))).hasSize(257);
	}

	@Test
	public void loweringMaxResultsRefusesWhatTheDefaultFolds() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		env.compile("probe(1, 2, 3)");
		assertThat(foldedValues(probe.arguments.get(0))).hasSize(3);

		env.compile("probe(1, 2, 3)", folding(ConstantFoldingOptions.newBuilder().setMaxResults(2).build()));
		assertThat(foldedValues(probe.arguments.get(1))).isNull();
	}

	@Test
	public void theEmbeddedRuntimeOptionsBoundWhatAFoldMayBuild() throws Exception {
		Probe probe = new Probe();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("probe", 1), probe));

		env.compile("probe([range(0; 300)])");
		assertThat(foldedValues(probe.arguments.get(0))).as("300 elements exceeds the default maxArrayLength").isNull();

		// Raising the array limit is not enough on its own: draining 300 values through the array
		// construction's own counter also has to fit maxOutputsPerExpression.
		ConstantFoldingOptions roomier = ConstantFoldingOptions.newBuilder()
				.setRuntimeOptions(RuntimeOptions.newBuilder()
						.setMaxArrayLength(512)
						.setMaxObjectMemberCount(256)
						.setMaxStringLength(4096)
						.setMaxOutputsPerExpression(512)
						.setMaxUserDefinedFunctionCalls(256)
						.build())
				.build();
		env.compile("probe([range(0; 300)])", folding(roomier));
		assertThat(foldedValues(probe.arguments.get(1))).hasSize(1);
	}

	@Test
	public void foldingCanBeTurnedOff() throws Exception {
		Tick tick = new Tick();
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.singletonMap(FunctionSignature.of("tick", 0), tick));

		JsonQuery<JsonNode> query = env.compile("[tick, tick + 1]",
				folding(ConstantFoldingOptions.newBuilder().setEnabled(false).build()));
		assertThat(tick.evaluations.get()).as("nothing is evaluated while compiling").isZero();

		// Same answers, evaluated afresh every time.
		for (int i = 1; i <= 3; i++) {
			assertThat(apply(query, "null")).extracting(Object::toString).containsExactly("[1,2]");
			assertThat(tick.evaluations.get()).isEqualTo(2 * i);
		}
	}

	@Test
	public void turningFoldingOffPutsConstantWorkBackUnderTheCallersRuntimeOptions() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());
		RuntimeOptions maxArrayLength100 = RuntimeOptions.newBuilder().setMaxArrayLength(100).build();

		// Folded, so the array is built while compiling and the runtime limit never sees it. This is
		// RuntimeOptionsTest#constantWorkIsBoundedByTheCompilerNotByTheseLimits from the other side.
		JsonQuery<JsonNode> folded = env.compile("[range(0; 101)]").withRuntimeOptions(maxArrayLength100);
		assertThatCode(() -> apply(folded, "null")).doesNotThrowAnyException();

		JsonQuery<JsonNode> unfolded = env
				.compile("[range(0; 101)]", folding(ConstantFoldingOptions.newBuilder().setEnabled(false).build()))
				.withRuntimeOptions(maxArrayLength100);
		assertThatThrownBy(() -> apply(unfolded, "null"))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum array size of 100");
	}

	@Test
	public void theDerivedTotalStillBoundsOneCompilation() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());

		// A constant term the folder refuses on size, so every enclosing fold attempt re-runs it from
		// scratch. Without a compilation-wide ceiling this is quadratic in the length of the query.
		StringBuilder query = new StringBuilder("[");
		for (int i = 0; i < 200; i++)
			query.append(i == 0 ? "" : ", ").append("reduce range(0; 300) as $x (0; .)");
		query.append("]");
		for (int i = 0; i < 200; i++)
			query.append(" | .");

		long started = System.nanoTime();
		assertThatCode(() -> env.compile(query.toString())).doesNotThrowAnyException();
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(10);
	}

	// --- folding is not visible in results --------------------------------------------------------

	@Test
	public void foldingPreservesValuesAndOrder() throws Exception {
		Environment<JsonNode> env = env(Versions.JQ_1_8_2, Collections.emptyMap());
		for (String[] pair : Arrays.asList(
				new String[] { "[limit(2; 1, 2, 3)]", "[1,2]" },
				new String[] { "[range(0; 3)]", "[0,1,2]" },
				new String[] { "\"\\(1 + 1)\"", "\"2\"" },
				new String[] { "[label $out | (1, 2, break $out, 3)]", "[1,2]" },
				new String[] { "[{a: 1, b: 2} | to_entries[] | .key]", "[\"a\",\"b\"]" },
				new String[] { "def f: 1 + 1; f", "2" })) {
			assertThat(apply(env.compile(pair[0]), "null")).as(pair[0]).extracting(Object::toString).containsExactly(pair[1]);
		}
	}
}
