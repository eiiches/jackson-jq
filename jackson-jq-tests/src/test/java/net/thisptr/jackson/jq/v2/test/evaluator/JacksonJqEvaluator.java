package net.thisptr.jackson.jq.v2.test.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JacksonJqEvaluator implements Evaluator {

	private Result doEvaluate(Expression<JsonNode> expr, JsonNode in) throws JsonQueryException {
		List<JsonNode> values = new ArrayList<>();
		try {
			expr.apply(null, in, null, (out, opath) -> {
				@Var JsonNode value = out;
				if (out.isNumber() && Double.isNaN(out.asDouble()))
					value = NullNode.getInstance();
				if (out.isNumber() && Double.isInfinite(out.asDouble()))
					value = DoubleNode.valueOf(out.asDouble() > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE);
				values.add(value);
			}, false);
			return new Result(values, null);
		} catch (Throwable th) {
			return new Result(values, th);
		}
	}

	@SuppressWarnings("deprecation")
	private static void terminateThread(Thread thread) {
		thread.stop();
	}

	@Override
	public Result evaluate(String exprText, JsonNode in, Version version, long timeout) throws Throwable {
		AtomicReference<Result> result = new AtomicReference<>();
		AtomicReference<Throwable> exception = new AtomicReference<>();
		Thread th = new Thread() {
			@Override
			public void run() {
				try {
					AstNode ast = AstParser.parse(exprText, version);
					Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), version);
					Expression<JsonNode> expr = Compiler.compile(env, ast);
					result.set(doEvaluate(expr, in));
				} catch (Throwable e) {
					exception.set(e);
				}
			}
		};
		th.start();
		th.join(timeout);
		if (th.isAlive()) {
			terminateThread(th);
			throw new TimeoutException("timeout");
		}
		if (exception.get() != null)
			throw exception.get();
		return Objects.requireNonNull(result.get());
	}
}
