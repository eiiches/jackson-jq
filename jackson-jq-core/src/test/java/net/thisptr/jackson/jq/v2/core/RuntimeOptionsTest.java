package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuntimeOptionsTest {
	private static final Environment<JsonNode> ENV = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2).build();

	private static List<JsonNode> run(String q, RuntimeOptions options) throws Exception {
		List<JsonNode> out = new ArrayList<>();
		ENV.compile(q).apply(Jackson2JsonProvider.getInstance().createNull(), options, out::add);
		return out;
	}

	private static RuntimeOptions maxArrayLength(int n) {
		return RuntimeOptions.newBuilder().setMaxArrayLength(n).build();
	}

	private static RuntimeOptions maxObjectMemberCount(int n) {
		return RuntimeOptions.newBuilder().setMaxObjectMemberCount(n).build();
	}

	private static RuntimeOptions maxStringLength(int n) {
		return RuntimeOptions.newBuilder().setMaxStringLength(n).build();
	}

	// --- defaults -----------------------------------------------------------------------------

	@Test
	public void defaultsAreUnlimited() throws Exception {
		RuntimeOptions defaults = RuntimeOptions.newBuilder().build();
		assertThat(defaults.getMaxArrayLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxObjectMemberCount()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxStringLength()).isEqualTo(Integer.MAX_VALUE);

		assertThat(run("[range(0; 100000)] | length", defaults)).containsExactly(Jackson2JsonProvider.getInstance().createNumber(100000));
		// The no-options overloads must behave identically.
		List<JsonNode> out = new ArrayList<>();
		ENV.compile("[range(0; 100000)] | length").apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(100000));
	}

	@Test
	public void eachSetterLeavesTheOtherLimitsAlone() {
		RuntimeOptions options = RuntimeOptions.newBuilder().setMaxObjectMemberCount(7).setMaxStringLength(5).setMaxArrayLength(3).build();
		assertThat(options.getMaxArrayLength()).isEqualTo(3);
		assertThat(options.getMaxObjectMemberCount()).isEqualTo(7);
		assertThat(options.getMaxStringLength()).isEqualTo(5);
	}

	// --- maxArrayLength -----------------------------------------------------------------------

	@Test
	public void arrayConstructionIsBounded() throws Exception {
		assertThatCode(() -> run("[range(0; 100)]", maxArrayLength(100))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("[range(0; 101)]", maxArrayLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum array size of 100");
	}

	@Test
	public void arrayConcatenationIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; 100) as $i ([]; . + [$i])", maxArrayLength(100))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; 101) as $i ([]; . + [$i])", maxArrayLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void setpathRejectsAHugeIndexWithoutAllocating() {
		long started = System.nanoTime();
		assertThatThrownBy(() -> run("setpath([1000000000]; 1)", maxArrayLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("Array of 1000000001 elements");
		// The point of pre-checking is that the billion-slot ArrayList is never allocated; if it were,
		// this would take seconds (or OOM) rather than microseconds.
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	@Test
	public void setpathWithinTheLimitStillExtendsArrays() throws Exception {
		// Compared as text: the engine's numeric node type need not match the parser's.
		assertThat(run("setpath([3]; 1)", maxArrayLength(4))).extracting(Object::toString).containsExactly("[null,null,null,1]");
	}

	@Test
	public void sliceAssignmentIsBounded() throws Exception {
		assertThatThrownBy(() -> run("[1, 2] | .[0:0] = [1, 2, 3]", maxArrayLength(4)))
				.isInstanceOf(RuntimeLimitExceededException.class);
		assertThatCode(() -> run("[1, 2] | .[0:0] = [1, 2]", maxArrayLength(4))).doesNotThrowAnyException();
	}

	// --- maxObjectMemberCount ------------------------------------------------------------------

	@Test
	public void objectMergeIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; 50) as $i ({}; . + {($i|tostring): $i})", maxObjectMemberCount(50))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; 51) as $i ({}; . + {($i|tostring): $i})", maxObjectMemberCount(50)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum object size of 50");
	}

	@Test
	public void recursiveObjectMergeIsBounded() throws Exception {
		assertThatThrownBy(() -> run("{a: 1, b: 2} * {c: 3}", maxObjectMemberCount(2)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void objectFieldAssignmentIsBounded() throws Exception {
		assertThatThrownBy(() -> run("reduce range(0; 51) as $i ({}; setpath([$i|tostring]; $i))", maxObjectMemberCount(50)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void fromEntriesIsBounded() throws Exception {
		// from_entries turns an array into an object, so maxArrayLength does not bound the result.
		assertThatThrownBy(() -> run("[range(0; 51) | {key: (.|tostring), value: .}] | from_entries", maxObjectMemberCount(50)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	// --- maxStringLength ----------------------------------------------------------------------

	@Test
	public void stringConcatenationIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; 100) as $i (\"\"; . + \"x\")", maxStringLength(100))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; 101) as $i (\"\"; . + \"x\")", maxStringLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum string length of 100");
	}

	@Test
	public void stringRepetitionIsRejectedWithoutAllocating() {
		long started = System.nanoTime();
		assertThatThrownBy(() -> run("\"x\" * 1000000000", maxStringLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("String of 1000000000 characters");
		// As with setpath, the point of pre-checking is that the billion-char buffer is never
		// allocated; if it were, this would take seconds (or OOM) rather than microseconds.
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	@Test
	public void stringInterpolationIsBounded() throws Exception {
		assertThatCode(() -> run("\"aaa\" | \"\\(.)\\(.)\"", maxStringLength(6))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("\"aaa\" | \"\\(.)\\(.)\"", maxStringLength(5)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void joinIsBounded() throws Exception {
		assertThatCode(() -> run("[\"aaa\", \"bbb\"] | join(\"-\")", maxStringLength(7))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("[\"aaa\", \"bbb\"] | join(\"-\")", maxStringLength(6)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void implodeIsBounded() throws Exception {
		assertThatThrownBy(() -> run("[range(0; 101) | 65] | implode", maxStringLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void formattingFunctionsAreBounded() throws Exception {
		// "aaaa" | @base64 is "YWFhYQ==", eight characters.
		assertThatCode(() -> run("\"aaaa\" | @base64", maxStringLength(8))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("\"aaaa\" | @base64", maxStringLength(7)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void xsvFormattingIsBounded() throws Exception {
		// ["aaa", "bbb"] | @csv is "\"aaa\",\"bbb\"", eleven characters.
		assertThatCode(() -> run("[\"aaa\", \"bbb\"] | @csv", maxStringLength(11))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("[\"aaa\", \"bbb\"] | @csv", maxStringLength(10)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void tojsonIsBounded() throws Exception {
		assertThatCode(() -> run("[1, 2, 3] | tojson", maxStringLength(7))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("[1, 2, 3] | tojson", maxStringLength(6)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void lengthIsCountedInUtf16CodeUnits() throws Exception {
		// An astral character is two chars but one codepoint, so the limit is stricter than jq's
		// length would suggest -- and length itself must keep reporting codepoints.
		assertThat(run("\"\uD83D\uDE00\" | length", RuntimeOptions.newBuilder().build())).extracting(Object::toString).containsExactly("1");
		assertThatCode(() -> run("\"\uD83D\uDE00\" + \"\"", maxStringLength(2))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("\"\uD83D\uDE00\" + \"\"", maxStringLength(1)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void literalsAndInputsAreNotBounded() throws Exception {
		// The limit bounds strings evaluation produces, not the ones it is handed.
		assertThatCode(() -> run("\"aaaaa\"", maxStringLength(1))).doesNotThrowAnyException();
	}

	// --- the SPI path -------------------------------------------------------------------------

	@Test
	public void aCustomFunctionSeesTheLimitsThroughItsContext() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("observed_max_array_length", 0), new Function() {
					@Override
					public <Context extends RuntimeContext, N> Expression<Context, N> bindArguments(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, net.thisptr.jackson.jq.v2.spi.version.Version ver) {
						return (context, in, path, output) -> output.emit(jsonProvider.createNumber(context.getRuntimeLimits().getMaxArrayLength()), UntrackedPath.getInstance());
					}
				})
				.build();

		JsonQuery<JsonNode> query = env.compile("observed_max_array_length");
		List<JsonNode> out = new ArrayList<>();
		query.apply(Jackson2JsonProvider.getInstance().createNull(), maxArrayLength(12), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(12));
	}

	// --- concurrency --------------------------------------------------------------------------

	@Test
	public void oneCompiledQueryHonoursPerInvocationLimits() throws Exception {
		JsonQuery<JsonNode> query = ENV.compile("[range(0; 50)] | length");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < 64; ++i) {
				boolean tight = i % 2 == 0;
				Callable<Boolean> task = () -> {
					List<JsonNode> out = new ArrayList<>();
					try {
						query.apply(Jackson2JsonProvider.getInstance().createNull(), tight ? maxArrayLength(10) : RuntimeOptions.newBuilder().build(), out::add);
						return false;
					} catch (RuntimeLimitExceededException e) {
						return true;
					}
				};
				futures.add(executor.submit(task));
			}
			for (int i = 0; i < futures.size(); ++i)
				assertThat(futures.get(i).get()).isEqualTo(i % 2 == 0);
		} finally {
			executor.shutdownNow();
		}
	}
}
