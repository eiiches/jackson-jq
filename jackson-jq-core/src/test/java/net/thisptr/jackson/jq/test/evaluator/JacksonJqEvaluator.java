package net.thisptr.jackson.jq.test.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.DefaultRootScope;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JacksonJqEvaluator implements Evaluator {

	private Result doEvaluate(final Expression expr, final JsonNode in, final Version version) throws JsonQueryException {
		final List<JsonNode> values = new ArrayList<>();
		final Scope scope = Scope.newChildScope(DefaultRootScope.getInstance(version));
		try {
			expr.apply(scope, in, null, (out, opath) -> {
				if (out.isNumber() && Double.isNaN(out.asDouble()))
					out = NullNode.getInstance();
				if (out.isNumber() && Double.isInfinite(out.asDouble()))
					out = DoubleNode.valueOf(out.asDouble() > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE);
				values.add(out);
			}, false);
			return new Result(values, null);
		} catch (final Throwable th) {
			return new Result(values, th);
		}
	}

	@SuppressWarnings("deprecation")
	private static void terminateThread(final Thread thread) {
		thread.stop();
	}

	@Override
	public Result evaluate(final String exprText, final JsonNode in, final Version version, final long timeout) throws Throwable {
		final AtomicReference<Result> result = new AtomicReference<>();
		final AtomicReference<Throwable> exception = new AtomicReference<>();
		final Thread th = new Thread() {
			@Override
			public void run() {
				try {
					final Expression expr = ExpressionParser.compile(exprText, version);
					result.set(doEvaluate(expr, in, version));
				} catch (final Throwable e) {
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
		return result.get();
	}
}