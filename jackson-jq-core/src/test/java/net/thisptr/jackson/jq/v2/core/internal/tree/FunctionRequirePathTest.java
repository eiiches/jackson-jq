package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.compile.Closure;
import net.thisptr.jackson.jq.v2.core.path.RootPath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.StackMemory;
import net.thisptr.jackson.jq.v2.spi.Version;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class FunctionRequirePathTest {

	@Test
	void doesNotPropagateRequirePathThroughFunctionCalls() {
		assertFalse(invokeResolvedFunctionCall());
		assertFalse(invokeLocalFunctionCall());
		assertFalse(invokeCapturedFunctionCall());
	}

	private boolean invokeResolvedFunctionCall() {
		AtomicBoolean requirePath = new AtomicBoolean(true);
		ResolvedFunctionCall<JsonNode> call = new ResolvedFunctionCall<>("f", recordingExpression(requirePath));

		call.apply(null, NullNode.getInstance(), RootPath.getInstance(), (out, path) -> {}, true);
		return requirePath.get();
	}

	private boolean invokeLocalFunctionCall() {
		AtomicBoolean requirePath = new AtomicBoolean(true);
		FunctionFactory factory = recordingFactory(requirePath);
		StackFrame frame = new StackMemory().pushFrame(1);
		frame.set(0, factory);
		ResolvedLocalFunctionAccess<JsonNode> call = new ResolvedLocalFunctionAccess<>(
				Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_7, "f", 0,
				Collections.emptyList(), null, null);

		call.apply(frame, NullNode.getInstance(), RootPath.getInstance(), (out, path) -> {}, true);
		return requirePath.get();
	}

	private boolean invokeCapturedFunctionCall() {
		AtomicBoolean requirePath = new AtomicBoolean(true);
		Closure closure = new Closure(1);
		closure.set(0, recordingFactory(requirePath));
		StackFrame frame = new StackMemory().pushFrame(1);
		frame.set(0, closure);
		ResolvedCapturedFunctionAccess<JsonNode> call = new ResolvedCapturedFunctionAccess<>(
				Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_7, "f", 0, 0,
				Collections.emptyList(), null, null);

		call.apply(frame, NullNode.getInstance(), RootPath.getInstance(), (out, path) -> {}, true);
		return requirePath.get();
	}

	private FunctionFactory recordingFactory(AtomicBoolean requirePath) {
		return new FunctionFactory() {
			@Override
			public <N> Expression<N> createFunction(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version version) {
				return recordingExpression(requirePath);
			}
		};
	}

	private <N> Expression<N> recordingExpression(AtomicBoolean requirePath) {
		return (frame, in, path, output, actualRequirePath) -> requirePath.set(actualRequirePath);
	}
}
