package net.thisptr.jackson.jq.v2.spi;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Scope<JsonNode> {
	private @Nullable JsonProvider<JsonNode> jsonProvider;

	public JsonProvider<JsonNode> jsonProvider() {
		if (jsonProvider != null)
			return jsonProvider;
		if (parentScope == null)
			throw new IllegalStateException("JsonProvider is not set");
		return parentScope.jsonProvider();
	}

	public void setJsonProvider(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	private @Nullable Scope<JsonNode> parentScope;

	public interface ValueWithPath<JsonNode> {
		@Nullable JsonNode value();

		@Nullable Path<JsonNode> path();
	}

	private ExecutionStack<JsonNode>.@Nullable Frame executionFrame;

	public ExecutionStack<JsonNode>.@Nullable Frame getExecutionFrame() {
		if (executionFrame != null)
			return executionFrame;
		if (parentScope != null)
			return parentScope.getExecutionFrame();
		return null;
	}

	private Scope(@Nullable Scope<JsonNode> parentScope) {
		this.parentScope = parentScope;
	}

	public static <JsonNode> Scope<JsonNode> newEmptyScope(@Nullable JsonProvider<JsonNode> jsonProvider) {
		Scope<JsonNode> scope = new Scope<>(null);
		if (jsonProvider != null)
			scope.setJsonProvider(jsonProvider);
		return scope;
	}

	public static <JsonNode> Scope<JsonNode> newChildScope(Scope<JsonNode> scope) {
		return new Scope<>(scope);
	}

	public static <JsonNode> Scope<JsonNode> newChildScopeWithFrame(Scope<JsonNode> scope, ExecutionStack<JsonNode>.Frame frame) {
		Scope<JsonNode> child = new Scope<>(scope);
		child.executionFrame = frame;
		return child;
	}

	public @Nullable JsonNode getValue(int slot) {
		ExecutionStack<JsonNode>.Frame frame = getExecutionFrame();
		if (frame != null) {
			JsonNode v = frame.getValueNode(slot);
			if (v != null)
				return v;
		}
		if (parentScope != null)
			return parentScope.getValue(slot);
		return null;
	}

	public @Nullable ValueWithPath<JsonNode> getValueWithPath(int slot) {
		ExecutionStack<JsonNode>.Frame frame = getExecutionFrame();
		if (frame != null) {
			ExecutionStack.PathAndValue<JsonNode> pv = frame.getValue(slot);
			if (pv != null) {
				return new ValueWithPath<JsonNode>() {
					@Override
					public @Nullable JsonNode value() {
						return pv.getValue();
					}

					@Override
					public @Nullable Path<JsonNode> path() {
						return pv.getPath();
					}
				};
			}
		}
		if (parentScope != null)
			return parentScope.getValueWithPath(slot);
		return null;
	}

	public void setValue(int slot, JsonNode value) {
		setValueWithPath(slot, value, null, 0);
	}

	public void setValue(int slot, JsonNode value, int frameSize) {
		setValueWithPath(slot, value, null, frameSize);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path) {
		setValueWithPath(slot, value, path, 0);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path, int frameSize) {
		if (slot < 0)
			return;
		@Var ExecutionStack<JsonNode>.Frame frame = getExecutionFrame();
		if (frame == null) {
			ExecutionStack<JsonNode> stack = new ExecutionStack<>();
			int size = Math.max(slot + 1, frameSize);
			executionFrame = stack.pushFrame(null, size);
			frame = executionFrame;
		}
		if (path != null) {
			frame.set(slot, new ExecutionStack.PathAndValue<>(path, value));
		} else {
			frame.set(slot, value);
		}
	}

	public @Nullable FunctionFactory getFunctionFactory(int slot) {
		ExecutionStack<JsonNode>.Frame frame = getExecutionFrame();
		if (frame != null) {
			FunctionFactory f = frame.getFunctionFactory(slot);
			if (f != null)
				return f;
		}
		if (parentScope != null)
			return parentScope.getFunctionFactory(slot);
		return null;
	}

	public void setFunctionFactory(int slot, FunctionFactory factory) {
		if (slot < 0)
			return;
		@Var ExecutionStack<JsonNode>.Frame frame = getExecutionFrame();
		if (frame == null) {
			ExecutionStack<JsonNode> stack = new ExecutionStack<>();
			executionFrame = stack.pushFrame(null, slot + 1);
			frame = executionFrame;
		}
		frame.set(slot, factory);
	}
}

