package net.thisptr.jackson.jq.v2.spi;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Factory for creating executable {@link Expression} instances.
 * <p>
 * A {@code Function} represents a compiled function expression bound to its lexical environment.
 * Any captured lexical state (such as a {@code Closure}) is encapsulated internally by the specific
 * {@code Function} implementation rather than supplied dynamically at invocation time by callers.
 * <p>
 * {@code Function} instances may be cached and shared by the caller across compilations. Implementations
 * must be safe for concurrent, reentrant calls to {@link #bind}.
 * <p>
 * This SPI does not specify which registration wins if multiple providers (or multiple
 * {@code @FunctionRegistration}s) resolve to the same {@code FunctionSignature}; which one is used
 * is left to the function/module loader in use (e.g. {@code ClassPathFunctionLoader} picks one
 * arbitrarily, based on {@link java.util.ServiceLoader} discovery order).
 */
public interface Function {
	/**
	 * Analyzes a complete call from the conservative properties of its ordered arguments.
	 * <p>
	 * The argument list is unmodifiable. Implementations decide which arguments are evaluated and
	 * whether they receive the call input; argument dependencies are therefore not implicitly added
	 * to the returned result. This method must be cheap, pure, thread-safe, and stable for identical
	 * inputs. A compiler may invoke it more than once.
	 */
	default ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionProperties.UNKNOWN;
	}

	/**
	 * Returns the overloads this function supports when compiled against {@code jqVersion}
	 * and invoked with {@code arity} arguments. Undeclared functions form a dynamic boundary
	 * for compatibility.
	 * <p>
	 * A builtin whose accepted or produced types differ between jq releases returns the signatures of
	 * the release it is being compiled for; one whose signatures do not vary ignores the parameter.
	 * Call sites ignore {@link TypeScheme}s whose parameter count does not match the call's arity.
	 * Fixed-arity functions may ignore {@code arity} and return their constant list of overloads.
	 * Variadic functions or functions with arity-dependent signatures may use {@code arity}
	 * to select or generate the applicable schemes.
	 * <p>
	 * The result must be stable for a given {@code (jqVersion, arity)} pair, since the engine
	 * reads it once per call site and retains it. It is read on every compilation, whether or not type
	 * checking is enabled, so implementations should return a precomputed list rather than build one
	 * per call.
	 *
	 * @param jqVersion the jq compatibility version the call is being compiled for
	 * @param arity the number of arguments at the call site
	 * @return the overloads supported by this function
	 */
	default List<TypeScheme<FunctionType>> types(Version jqVersion, int arity) {
		if (arity < 0)
			throw new IllegalArgumentException("arity must not be negative");
		Type inputType = AnyType.getInstance();
		Type outputType = AnyType.getInstance();
		List<FilterType> parameterTypes = Collections.nCopies(arity, FilterType.of(AnyType.getInstance(), AnyType.getInstance()));
		return List.of(TypeScheme.of(FunctionType.of(inputType, outputType, parameterTypes)));
	}

	/**
	 * Binds the given arguments to produce an executable {@link Expression}. A compiler invokes this
	 * expensive operation exactly once for each statically resolved call node, after analysis and
	 * argument optimization. Dynamically supplied jq functions may still be bound at runtime.
	 * <p>
	 * The returned {@code Expression} may be retained by the caller and evaluated repeatedly, including
	 * concurrently, for the lifetime of the compiled query. It must not carry unsynchronized mutable
	 * per-call state.
	 * <p>
	 * {@code args} is ordered, stable, and unmodifiable. Implementations may keep a reference to it and
	 * read from it later (for example from within the returned {@code Expression}) without defensively
	 * copying it; attempting to mutate it throws {@link UnsupportedOperationException}.
	 * <p>
	 * Everything else the engine supplies arrives through {@code bindCtx} rather than as a parameter of its
	 * own, so that a later release can hand implementations more without changing this signature. An
	 * implementation that uses the {@link BindContext#getJsonProvider() provider} or the
	 * {@link BindContext#getJqVersion() jq version} from within the returned {@code Expression} should
	 * read them into local variables here and capture those, rather than capturing {@code bindCtx} and
	 * dereferencing it on every evaluation.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param <Context> the execution state that has to be passed on when evaluating function arguments; opaque
	 * apart from the {@link RuntimeContext#getRuntimeLimits() limits} it exposes
	 * @param bindCtx the compilation state this call is bound in
	 * @param args the argument expressions to bind
	 * @return the bound expression
	 */
	<Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) throws JsonQueryException;
}
