package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.BoundJqFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.core.internal.compile.DeferredJqFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedFunctionBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedCapturedVariableBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFixedVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedFunctionDefinition;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedGlobalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedGlobalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalFunctionAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalFunctionBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalVariableAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.ResolvedLocalVariableBoundArgumentAccess;
import net.thisptr.jackson.jq.v2.core.internal.compile.resolved.UnboundFunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.AbstractDelegatingExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.BreakExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Comma;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.FoldedConstantExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.FoldedErrorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ForeachExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.IdentifierKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.JsonQueryKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.Label;
import net.thisptr.jackson.jq.v2.core.internal.tree.NegativeExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.ObjectConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.RecursionOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ReduceExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.SemicolonOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringInterpolation;
import net.thisptr.jackson.jq.v2.core.internal.tree.StringKeyFieldConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TopLevelExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.VariableBinding;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AbstractBinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AlternativeOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanAndExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanOrExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.DivideExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MinusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.ModuloExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MultiplyExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.PlusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.AbstractComplexAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.Assignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexAlternativeAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexDivideAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMinusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexModuloAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMultiplyAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexPlusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.UpdateAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.AbstractComparisonExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareNotEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Type inference and validation over the resolved expression tree.
 * <p>
 * Every node this can meet has a rule below. There is deliberately no default branch: a node reaching here
 * without one is a compiler bug, not a query this should quietly type {@code ANY}, and saying so out loud is
 * what keeps a newly added construct from going unchecked. Only what genuinely publishes no type information
 * is typed {@code ANY} -- a function the environment supplied, or a variable it registered without naming a
 * type for it.
 * <p>
 * This runs before constant folding and before {@code StaticCallFinalizer} binds calls, so a third-party
 * {@code Function}'s own expressions are never in the tree; a call to one is an {@link UnboundFunctionCall},
 * typed from the schemes the function publishes.
 */
public final class TypeCheck {
	/**
	 * How many times a loop accumulator or a recursive definition is re-analysed before its still-changing
	 * parts are given up on. Every shape that settles at all settles well inside this.
	 */
	private static final int FIXED_POINT_ITERATIONS = 8;

	/**
	 * How many leading positions an array literal describes one by one. A tuple that anyone writes by hand
	 * is far shorter than this; past it, a generated literal would only make the type expensive to carry
	 * around for no gain.
	 */
	private static final int MAX_KNOWN_ELEMENTS = 32;

	/**
	 * How deep calls may nest before the analysis stops following them. A recursion whose input type keeps
	 * changing -- {@code flatten}, which reduces into an accumulator that grows on every round -- would
	 * otherwise present a call this has not seen before every time, and there would be nothing to stop it.
	 */
	private static final int MAX_CALL_DEPTH = 64;

	/**
	 * How many calls a diagnostic's trace names before it stops listing them. A trace is there to say which
	 * call the reader has to look at, and the calls nearest the failure are the ones that say it.
	 */
	private static final int MAX_TRACE_FRAMES = 8;

	private final TypeCheckMode mode;
	private final @Nullable DiagnosticListener listener;
	private final Map<AnalyzedExpression<?>, SourceLocation> locations;
	private final Map<Integer, Type> variables = new HashMap<>();
	private final Map<Integer, List<TypeScheme<FunctionType>>> functions = new HashMap<>();
	private final Map<Integer, ResolvedFunctionDefinition<?>> definitions = new HashMap<>();
	// A call site's result, keyed by everything the body's analysis depends on, so a definition reached
	// through several call sites of the same shape is analysed once. Without it, nesting definitions
	// multiplies: `def a: .; def b: a|a|a; def c: b|b|b; c` would analyse a body 27 times.
	private final Map<Specialization, Type> specializations = new HashMap<>();
	// Bodies currently being analysed, with the result a re-entrant call is to assume.
	private final Map<Specialization, Type> activeDefinitions = new HashMap<>();
	private final Set<Specialization> recursiveDefinitions = new HashSet<>();
	// The argument a parameter slot was called with, and the bindings in force where it was written.
	private final Map<Integer, Argument> arguments = new HashMap<>();
	private final AssignmentEffects assignments = new AssignmentEffects(this);
	// What the generic pass over each definition body found, held back until it is known whether anything
	// calls that definition. A def the query goes on to call is analysed again at the call site, with the
	// types it is really given; what the generic pass made of a bare type variable there is noise.
	// What each definition's closure holds, worked out where the definition was written. A closure slot
	// naming a variable resolves to its type there; one naming a function resolves to the frame slot the
	// definition it refers to occupies, which is how a def reaches itself and how a nested def reaches the
	// one enclosing it.
	private final Map<Integer, Closures> definitionClosures = new HashMap<>();
	private final Deque<Closures> closures = new ArrayDeque<>();
	private final Map<Integer, List<Diagnostic>> deferredDiagnostics = new HashMap<>();
	private final Deque<Integer> definitionsBeingInferred = new ArrayDeque<>();
	private final Set<Integer> specializedDefinitions = new HashSet<>();
	// The calls the analysis is currently inside, innermost first, and how many of them are bodies the
	// caller cannot see. Every diagnostic is reported with this trace and, once a body is opaque, at the
	// call site that entered it rather than at an expression the caller never wrote.
	private final Deque<Frame> frames = new ArrayDeque<>();
	private int opaqueFrames;
	// Where an argument was written, for the arguments that travel into a body. An argument is the caller's
	// own code wherever it ends up running, so its diagnostics belong to the trace it was written under, not
	// to the one the body reached it with.
	private final Map<AnalyzedExpression<?>, List<Frame>> argumentTraces = new IdentityHashMap<>();
	private int errors;
	private int freshVariables;
	private boolean guarded;
	private boolean inferenceContext;

	private TypeCheck(Map<AnalyzedExpression<?>, SourceLocation> locations, CompileOptions options) {
		this.locations = locations;
		this.mode = options.getTypeCheckMode();
		this.listener = options.getDiagnosticListener();
	}

	public static Type run(@Nullable AnalyzedExpression<?> expression,
						   Map<AnalyzedExpression<?>, SourceLocation> locations, CompileOptions options) throws JsonQueryException {
		if (options.getTypeCheckMode() == TypeCheckMode.OFF || expression == null)
			return AnyType.getInstance();
		TypeCheck check = new TypeCheck(locations, options);
		Type result = check.infer(expression, options.getInputType());
		check.checkDeclaredOutput(expression, result, options.getOutputType());
		check.reportUncalledDefinitions();
		if (check.errors != 0 && options.getTypeCheckMode() == TypeCheckMode.STRICT)
			throw new TypeCheckException(check.errors);
		return result;
	}

	/**
	 * Reports the caller's declared output type, if what the query is inferred to produce does not fit
	 * it. An inferred ANY fits anything, so a query the analysis cannot pin down is never faulted here.
	 */
	private void checkDeclaredOutput(AnalyzedExpression<?> expression, Type inferred, Type declared) {
		if (declared == AnyType.getInstance() || TypeMatcher.accepts(declared, inferred))
			return;
		report(expression, "Output type " + rejectedPart(declared, inferred)
				+ " is not assignable to the declared output type " + declared);
	}

	/**
	 * Which part of an inferred output the declaration turns down. A union is accepted only if every
	 * alternative is, so naming the alternatives that are not saves the reader comparing two long
	 * unions member by member.
	 */
	private static Type rejectedPart(Type declared, Type inferred) {
		if (!(inferred instanceof UnionType union))
			return inferred;
		List<Type> rejected = new ArrayList<>();
		for (Type alternative : union.alternatives())
			if (!TypeMatcher.accepts(declared, alternative))
				rejected.add(alternative);
		return rejected.isEmpty() ? inferred : UnionType.of(rejected);
	}

	Type infer(AnalyzedExpression<?> expression, Type input) {
		if (input == NeverType.getInstance())
			return NeverType.getInstance();
		try {
			return inferUnchecked(expression, input);
		} catch (TypeRelations.Problem problem) {
			report(expression, message(problem), problem.callee());
			return guarded ? NeverType.getInstance() : AnyType.getInstance();
		}
	}

	private Type inferUnchecked(AnalyzedExpression<?> expression, Type input) {
		if (expression instanceof AbstractDelegatingExpression<?> delegating)
			return infer(delegating.inner(), input);
		if (expression instanceof TopLevelExpression<?>)
			return infer(onlyChild(expression), input);
		if (expression instanceof ThisObject<?> || expression instanceof ValueLiteral<?>)
			return applyFilterSchemes(expression, input);
		if (expression instanceof PipedQuery<?>) {
			List<AnalyzedExpression<?>> children = children(expression);
			return infer(children.get(1), infer(children.get(0), input));
		}
		if (expression instanceof ArrayConstruction<?> array)
			return arrayConstruction(array, input);
		if (expression instanceof ObjectConstruction<?> object)
			return object(object, input);
		if (expression instanceof IdentifierFieldAccess<?> field)
			return guarded(expression, () -> field(expression, infer(field.target(), input), field.field()), field.permissive());
		if (expression instanceof StringFieldAccess<?> field)
			return guarded(expression, () -> stringField(field, input), field.permissive());
		if (expression instanceof BracketFieldAccess<?> bracket)
			return guarded(expression, () -> bracket(bracket, input), bracket.permissive());
		if (expression instanceof BracketExtractFieldAccess<?> extract)
			return guarded(expression, () -> TypeRelations.iterate(infer(extract.target(), input)), extract.permissive());
		if (expression instanceof NegativeExpression<?>)
			return TypeRelations.negate(infer(onlyChild(expression), input));
		if (expression instanceof Comma<?>) {
			List<Type> outputs = new ArrayList<>();
			for (AnalyzedExpression<?> child : children(expression))
				outputs.add(infer(child, input));
			return UnionType.of(outputs);
		}
		if (expression instanceof PlusExpression<?>)
			return binary(expression, input, TypeRelations::plus);
		if (expression instanceof MinusExpression<?>)
			return binary(expression, input, TypeRelations::minus);
		if (expression instanceof MultiplyExpression<?>)
			return binary(expression, input, TypeRelations::multiply);
		if (expression instanceof DivideExpression<?>)
			return binary(expression, input, TypeRelations::divide);
		if (expression instanceof ModuloExpression<?>)
			return binary(expression, input, TypeRelations::modulo);
		if (expression instanceof Assignment<?> assignment)
			return assignment(assignment, input);
		if (expression instanceof UpdateAssignment<?> assignment)
			return updateAssignment(assignment, input);
		if (expression instanceof AbstractComplexAssignment<?> assignment)
			return complexAssignment(assignment, input);
		if (expression instanceof AbstractComparisonExpression<?> || expression instanceof BooleanAndExpression<?>
				|| expression instanceof BooleanOrExpression<?>) {
			inferBinaryChildren(expression, input);
			return BooleanType.getInstance();
		}
		if (expression instanceof AlternativeOperatorExpression<?>) {
			List<Type> operands = inferBinaryChildren(expression, input);
			Type left = TypeRelations.withoutNull(operands.get(0));
			return left == NeverType.getInstance() ? operands.get(1) : UnionType.of(left, operands.get(1));
		}
		if (expression instanceof Conditional<?>)
			return conditional(expression, input);
		if (expression instanceof TryCatch<?>)
			return tryCatch(expression, input);
		if (expression instanceof StringInterpolation<?>) {
			for (AnalyzedExpression<?> child : children(expression))
				infer(child, input);
			return StringType.getInstance();
		}
		if (expression instanceof SemicolonOperator<?>) {
			@Var Type result = NeverType.getInstance();
			for (AnalyzedExpression<?> child : children(expression))
				result = infer(child, input);
			return result;
		}
		if (expression instanceof VariableBinding<?> binding)
			return binding(binding, input);
		if (expression instanceof ReduceExpression<?> reduce)
			return reduce(reduce, input);
		if (expression instanceof ForeachExpression<?> foreach)
			return foreach(foreach, input);
		if (expression instanceof RecursionOperator<?> recursion)
			return Descendants.of(input, recursion.visitsNullValues());
		if (expression instanceof Label<?> label)
			return infer(label.body(), input);
		// `break` unwinds to its label and emits nothing on the way.
		if (expression instanceof BreakExpression<?>)
			return NeverType.getInstance();
		if (expression instanceof ResolvedLocalVariableAccess<?> variable)
			return variables.getOrDefault(variable.slot(), AnyType.getInstance());
		if (expression instanceof ResolvedLocalVariableBoundArgumentAccess<?> variable)
			return variables.getOrDefault(variable.slot(), AnyType.getInstance());
		if (expression instanceof ResolvedCapturedVariableAccess<?> variable)
			return capturedVariable(variable.closureSlot());
		if (expression instanceof ResolvedCapturedVariableBoundArgumentAccess<?> variable)
			return capturedVariable(variable.closureSlot());
		// A variable the environment supplied. It publishes whatever type was registered with it, which is
		// ANY unless the registration named one.
		if (expression instanceof ResolvedFixedVariableAccess<?> variable)
			return variable.type();
		if (expression instanceof ResolvedGlobalVariableAccess<?> variable)
			return variable.type();
		if (expression instanceof ResolvedFunctionDefinition<?> definition)
			return define(definition);
		if (expression instanceof ResolvedLocalFunctionAccess<?> call)
			return localCall(call, call.name(), call.slot(), call.args(), input);
		if (expression instanceof ResolvedLocalFunctionBoundArgumentAccess<?> call)
			return localCall(call, call.name(), call.slot(), call.args(), input);
		if (expression instanceof ResolvedCapturedFunctionAccess<?> call)
			return capturedCall(call, call.name(), call.closureSlot(), call.args(), input);
		if (expression instanceof ResolvedCapturedFunctionBoundArgumentAccess<?> call)
			return capturedCall(call, call.name(), call.closureSlot(), call.args(), input);
		// A function the environment supplied publishes nothing to specialize on.
		if (expression instanceof ResolvedGlobalFunctionAccess<?> call)
			return opaqueCall(call.args(), input);
		if (expression instanceof UnboundFunctionCall<?> call)
			return applySchemes(expression, call.name(), call.args().size(), call.args(), input, call.typeSchemes());
		if (expression instanceof BoundJqFunctionCall<?> call)
			return boundJqCall(call, input);
		// A jq function calling itself while its own body is still being compiled: there is no body here to
		// look at, only the arguments.
		if (expression instanceof DeferredJqFunctionCall<?> call)
			return opaqueCall(call.arguments(), input);
		throw new IllegalStateException("Missing type rule for " + expression.getClass().getName());
	}

	private Type object(ObjectConstruction<?> object, Type input) {
		Map<String, Type> fields = new TreeMap<>();
		@Var Type additional = NeverType.getInstance();
		for (FieldConstruction<?> field : object.fields()) {
			@Nullable String key;
			Type value;
			if (field instanceof IdentifierKeyFieldConstruction<?> identifier) {
				key = identifier.key;
				value = identifier.value != null ? infer(identifier.value, input) : TypeRelations.field(input, key);
			} else if (field instanceof StringKeyFieldConstruction<?> stringField) {
				key = objectKey(stringField.key, input);
				value = stringField.value != null ? infer(stringField.value, input)
						: key != null ? TypeRelations.field(input, key) : AnyType.getInstance();
			} else if (field instanceof JsonQueryKeyFieldConstruction<?> queryField) {
				key = objectKey(queryField.key(), input);
				value = infer(queryField.value(), input);
			} else {
				throw new IllegalStateException("Missing type rule for object field " + field.getClass().getName());
			}
			// A key only known at runtime says nothing about which field it names, so its value joins the
			// additional-field type rather than declaring one. Only the value type does: mixing the key's
			// type in would claim the object can hold strings it cannot.
			if (key != null)
				fields.put(key, value);
			else
				additional = UnionType.of(additional, value);
		}
		return ObjectType.of(fields, additional);
	}

	/**
	 * Validates an object-construction key and returns it when it is statically a single known string --
	 * including through a parenthesised constant expression, which is why {@code {("a"): 1}} declares a
	 * field rather than widening the object.
	 */
	private @Nullable String objectKey(AnalyzedExpression<?> key, Type input) {
		Type keyType = infer(key, input);
		require(StringType.getInstance(), keyType, "Object key must be a string, not " + keyType);
		return stringLiteral(key);
	}

	private Type field(AnalyzedExpression<?> expression, Type target, String name) {
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof ObjectType object && object.isClosed() && !object.fields().containsKey(name)) {
				warn(expression, "Field \"" + name + "\" does not exist in closed object " + object);
				break;
			}
		}
		return TypeRelations.field(target, name);
	}

	private Type stringField(StringFieldAccess<?> access, Type input) {
		Type target = infer(access.target(), input);
		Type keyType = infer(access.key(), input);
		require(StringType.getInstance(), keyType, "Field name must be a string, not " + keyType);
		@Nullable String key = stringLiteral(access.key());
		return key != null ? field(access, target, key) : dynamicMember(target);
	}

	private static Type dynamicMember(Type target) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ObjectType object)
				outputs.add(TypeRelations.objectMemberOrNull(object));
			else if (alternative instanceof NullType)
				outputs.add(NullType.getInstance());
			else
				throw new TypeRelations.Problem("Cannot index " + alternative + " with a string");
		}
		return UnionType.of(outputs);
	}

	/**
	 * An array literal whose elements each emit exactly one value builds an array that knows what is at
	 * each position -- {@code [$n, null]} is a count and an item, not an array of number-or-null. Anything
	 * generating an unknown number of values says only what kind of element the array holds.
	 */
	private Type arrayConstruction(ArrayConstruction<?> array, Type input) {
		if (array.q == null)
			return ArrayType.of(NeverType.getInstance());
		List<AnalyzedExpression<?>> elements = commaOperands(array.q);
		List<Type> elementTypes = new ArrayList<>(elements.size());
		@Var
		boolean positional = elements.size() <= MAX_KNOWN_ELEMENTS;
		for (AnalyzedExpression<?> element : elements) {
			Type elementType = infer(element, input);
			elementTypes.add(elementType);
			// NEVER means the literal never finishes, which is not something one position can say.
			if (element.getCardinality() != Cardinality.ONE || elementType == NeverType.getInstance())
				positional = false;
		}
		return positional ? ArrayType.of(elementTypes) : ArrayType.of(UnionType.of(elementTypes));
	}

	/**
	 * The operands of a comma chain, flattened, or the expression itself when it is not one.
	 */
	private static List<AnalyzedExpression<?>> commaOperands(AnalyzedExpression<?> expression) {
		AnalyzedExpression<?> unwrapped = unwrap(expression);
		if (!(unwrapped instanceof Comma<?>))
			return List.of(unwrapped);
		List<AnalyzedExpression<?>> operands = new ArrayList<>();
		for (AnalyzedExpression<?> child : children(unwrapped))
			operands.addAll(commaOperands(child));
		return operands;
	}

	private Type bracket(BracketFieldAccess<?> bracket, Type input) {
		Type target = infer(bracket.target(), input);
		if (bracket.isRange()) {
			Type bound = UnionType.of(NumericType.getInstance(), NullType.getInstance());
			Type start = infer(bracket.startExpr(), input);
			Type end = infer(bracket.endExpr(), input);
			require(bound, start, "Slice start must be a number or null, not " + start);
			require(bound, end, "Slice end must be a number or null, not " + end);
			return TypeRelations.slice(target);
		}
		Type index = infer(bracket.startExpr(), input);
		@Nullable String key = stringLiteral(bracket.startExpr());
		List<Type> outputs = new ArrayList<>();
		// Both sides are ranged over: `.[(1, "a")]` indexes with a number on one branch and a string on the
		// other, and each has to meet every alternative of the target on its own.
		for (Type alternative : TypeRelations.alternatives(target)) {
			for (Type single : TypeRelations.alternatives(index))
				outputs.add(index(bracket, alternative, single, key));
		}
		return UnionType.of(outputs);
	}

	private Type index(BracketFieldAccess<?> bracket, Type target, Type index, @Nullable String key) {
		if (target instanceof AnyType || index instanceof AnyType)
			return AnyType.getInstance();
		if (target instanceof NullType)
			return NullType.getInstance();
		if (target instanceof ArrayType array && index instanceof NumericType)
			return element(array, integerLiteral(bracket.startExpr()));
		// `.[[x]]` answers the positions at which the subsequence occurs, so its result is index numbers.
		if (target instanceof ArrayType array && index instanceof ArrayType subsequence) {
			if (!TypeMatcher.accepts(array.elementType(), subsequence.elementType()))
				warn(bracket, "Subsequence element type " + subsequence.elementType()
						+ " cannot match array element type " + array.elementType());
			return ArrayType.of(NumericType.getInstance());
		}
		if (target instanceof ObjectType object && index instanceof StringType)
			return key != null ? field(bracket, object, key) : TypeRelations.objectMemberOrNull(object);
		throw new TypeRelations.Problem("Cannot index " + target + " with " + index);
	}

	private Type binding(VariableBinding<?> binding, Type input) {
		Type value = infer(binding.value(), input);
		return withPatternBindings(binding.matcher(), value, () -> infer(binding.body(), input));
	}

	/**
	 * Runs {@code body} with every variable {@code matcher} binds in scope, restoring what they shadowed.
	 */
	<R> R withPatternBindings(PatternMatcher<?> matcher, Type value, Supplier<R> body) {
		Map<Integer, Type> bindings = new HashMap<>();
		bindPattern(matcher, value, bindings);
		Map<Integer, Type> previous = installVariables(bindings);
		try {
			return body.get();
		} finally {
			restoreVariables(bindings.keySet(), previous);
		}
	}

	// A matcher's JsonNode parameter says nothing about types; the analysis is the same whichever provider
	// compiled it, and the wildcard is only there because TypeCheck itself is not generic.
	@SuppressWarnings("unchecked")
	private void bindPattern(PatternMatcher<?> matcher, Type value, Map<Integer, Type> bindings) {
		PatternTypes.bind(this, (PatternMatcher<Object>) matcher, value, bindings);
	}

	/**
	 * {@code reduce SOURCE as $x (INIT; UPDATE)}: the accumulator starts at INIT and each item replaces it
	 * with UPDATE's value, so the result is whatever the accumulator can settle at.
	 */
	private Type reduce(ReduceExpression<?> reduce, Type input) {
		Type item = infer(reduce.iterExpr(), input);
		Type initial = infer(reduce.initExpr(), input);
		return withPatternBindings(reduce.matcher(), item, () ->
				accumulate(reduce, initial, accumulator -> {
					Type updated = emptyAsNull(reduce.reduceExpr(), infer(reduce.reduceExpr(), accumulator));
					return new Iteration(updated, updated);
				}).output);
	}

	/**
	 * {@code foreach SOURCE as $x (INIT; UPDATE; EXTRACT)}: like reduce, except that every iteration emits,
	 * and what it emits is EXTRACT of the updated accumulator -- never INIT, which is only ever a starting
	 * point.
	 */
	private Type foreach(ForeachExpression<?> foreach, Type input) {
		Type item = infer(foreach.iterExpr(), input);
		Type initial = infer(foreach.initExpr(), input);
		AnalyzedExpression<?> extract = foreach.extractExpr();
		Accumulation result = withPatternBindings(foreach.matcher(), item, () ->
				accumulate(foreach, initial, accumulator -> {
					Type updated = emptyAsNull(foreach.updateExpr(), infer(foreach.updateExpr(), accumulator));
					return new Iteration(updated, extract != null ? infer(extract, updated) : updated);
				}));
		// A source that emits nothing runs no iteration, so nothing is emitted either.
		return foreach.iterExpr().getCardinality() == Cardinality.ZERO ? NeverType.getInstance() : result.emitted();
	}

	/**
	 * Re-runs one iteration until the accumulator stops changing, keeping its shape rather than growing a
	 * union alternative per pass, and closing a shape that has begun to embed itself into a recursive type.
	 */
	private Accumulation accumulate(AnalyzedExpression<?> owner, Type initial, Iterate iterate) {
		@Var Type accumulator = initial;
		@Var Type emitted = NeverType.getInstance();
		for (int i = 0; i < FIXED_POINT_ITERATIONS; i++) {
			Iteration iteration = iterate.run(accumulator);
			emitted = StructuralWidening.join(emitted, iteration.emitted);
			Type joined = StructuralWidening.join(accumulator, StructuralWidening.join(initial, iteration.updated));
			Type next = StructuralWidening.fold(accumulator, joined, () -> fresh("Loop"));
			if (TypeEquivalence.isEqualType(next, accumulator))
				return new Accumulation(next, emitted);
			accumulator = next;
		}
		warn(owner, "Accumulator type did not settle; widening the parts that keep changing to ANY");
		Iteration iteration = iterate.run(accumulator);
		Type settled = StructuralWidening.widenDifferences(accumulator,
				StructuralWidening.join(initial, iteration.updated));
		return new Accumulation(settled, StructuralWidening.join(emitted, iteration.emitted));
	}

	/**
	 * An update that emits nothing leaves the accumulator null -- confirmed against every installed jq, and
	 * against {@code ReduceExpression}. {@link Cardinality#UNKNOWN} also covers "one or more", so this errs
	 * towards admitting a null the loop may never produce; the enum draws no finer line.
	 * <p>
	 * An update that emits nothing only by breaking out of the loop is the exception. {@code limit} is
	 * written that way, and the null would be a false one: the iteration that broke never hands a state to
	 * the next one, because there is no next one.
	 */
	private static Type emptyAsNull(AnalyzedExpression<?> update, Type updated) {
		if (update.getCardinality() == Cardinality.ONE || onlyEscapesByBreaking(update))
			return updated;
		return UnionType.of(updated, NullType.getInstance());
	}

	/**
	 * Whether every way {@code update} has of emitting nothing unwinds past the loop rather than finishing
	 * without a value.
	 * <p>
	 * Answered by looking for the constructs that emit nothing, rather than by working out which of them a
	 * given run reaches: anything other than a {@code break} leaving the update answers no, so a nested
	 * {@code empty} the update would never have reached still keeps the null. That is the side to be wrong
	 * on -- it is what this did for every update before {@code break} was told apart at all.
	 */
	private static boolean onlyEscapesByBreaking(AnalyzedExpression<?> update) {
		List<AnalyzedExpression<?>> pending = new ArrayList<>(List.of(update));
		Set<String> labels = new HashSet<>();
		List<BreakExpression<?>> breaks = new ArrayList<>();
		while (!pending.isEmpty()) {
			AnalyzedExpression<?> expression = unwrap(pending.remove(pending.size() - 1));
			if (expression instanceof BreakExpression<?> unwind) {
				breaks.add(unwind);
			} else {
				if (expression instanceof Label<?> label)
					labels.add(label.name());
				else if (expression.getCardinality() == Cardinality.ZERO)
					return false;
				pending.addAll(children(expression));
			}
		}
		@Var
		boolean escapes = false;
		for (BreakExpression<?> unwind : breaks) {
			// A label the update binds itself is one the update also catches, so breaking to it leaves the
			// update with no value rather than ending the loop.
			if (labels.contains(unwind.name()))
				return false;
			escapes = true;
		}
		return escapes;
	}

	/**
	 * Drops what a definition's own signature cannot speak for. A nested def that reads a variable of the
	 * def enclosing it types that variable by the enclosing def's input variable, which the nested def's
	 * scheme does not quantify -- and a scheme quantifying it anyway would capture the enclosing def's.
	 * Whatever the nested def says about it is only true of the enclosing analysis, so its signature says
	 * nothing about it instead.
	 */
	private static Type generalize(Type output, Collection<TypeVariable> quantified) {
		Set<TypeVariable> foreign = new HashSet<>(TypeSubstitution.freeVariables(output));
		foreign.removeAll(quantified);
		return foreign.isEmpty() ? output : TypeSubstitution.apply(output, foreign, Map.of());
	}

	private Type capturedVariable(int closureSlot) {
		Closures enclosing = closures.peek();
		if (enclosing == null)
			return AnyType.getInstance();
		return enclosing.variables().getOrDefault(closureSlot, AnyType.getInstance());
	}

	/**
	 * A call to a function the enclosing {@code def} closed over -- which is how a definition reaches
	 * itself, since its own {@link net.thisptr.jackson.jq.v2.spi.Function} is installed in the definer's
	 * slot before the closure is built.
	 */
	private Type capturedCall(AnalyzedExpression<?> call, String name, int closureSlot,
							  List<? extends AnalyzedExpression<?>> args, Type input) {
		Closures enclosing = closures.peek();
		Integer slot = enclosing == null ? null : enclosing.functions().get(closureSlot);
		if (slot == null)
			return opaqueCall(args, input);
		return localCall(call, name, slot, args, input);
	}

	/**
	 * Works out what a definition's closure will hold, from the scope it is written in. A hop-1 capture
	 * reads the definer's frame directly; a deeper one reads the definer's own closure, which is the entry
	 * currently in scope here.
	 */
	private Closures closuresOf(ResolvedFunctionDefinition<?> definition) {
		Closures definer = closures.peek();
		Map<Integer, Type> capturedVariables = new HashMap<>();
		for (ClosureSpec.CapturedVariableRef ref : definition.closureSpec().capturedVariables()) {
			Type type = ref.isLocalInParent ? variables.get(ref.parentSlot)
					: definer == null ? null : definer.variables().get(ref.parentSlot);
			if (type != null)
				capturedVariables.put(ref.targetSlot, type);
		}
		Map<Integer, Integer> capturedFunctions = new HashMap<>();
		Map<Integer, Argument> capturedArguments = new HashMap<>();
		for (ClosureSpec.CapturedFunctionRef ref : definition.closureSpec().capturedFunctions()) {
			Integer slot;
			if (ref.isLocalInParent) {
				slot = ref.parentSlot;
			} else {
				slot = definer == null ? null : definer.functions().get(ref.parentSlot);
				if (slot == null)
					continue;
			}
			capturedFunctions.put(ref.targetSlot, slot);
			Argument argument = arguments.get(slot);
			if (argument != null)
				capturedArguments.put(slot, argument);
			else if (definer != null && definer.capturedArguments().containsKey(slot))
				capturedArguments.put(slot, definer.capturedArguments().get(slot));
		}
		return new Closures(capturedVariables, capturedFunctions, capturedArguments);
	}

	private Type define(ResolvedFunctionDefinition<?> definition) {
		definitions.put(definition.slot(), definition);
		definitionClosures.put(definition.slot(), closuresOf(definition));
		TypeVariable input = fresh("Input");
		List<FilterType> parameters = new ArrayList<>();
		Map<TypeVariable, Type> quantified = new LinkedHashMap<>();
		quantified.put(input, AnyType.getInstance());
		Map<Integer, Type> previous = new HashMap<>();
		for (int i = 0; i < definition.paramNames().size(); i++) {
			String name = definition.paramNames().get(i);
			TypeVariable value = fresh(name + "Value");
			quantified.put(value, AnyType.getInstance());
			parameters.add(FilterType.of(input, value));
			int slot = definition.paramSlots().get(i);
			if (variables.containsKey(slot))
				previous.put(slot, variables.get(slot));
			variables.put(slot, value);
		}
		functions.put(definition.slot(), dynamicFunction(definition.paramNames().size()));
		Type output;
		boolean previousInferenceContext = inferenceContext;
		inferenceContext = true;
		definitionsBeingInferred.push(definition.slot());
		closures.push(definitionClosures.get(definition.slot()));
		try {
			output = infer(definition.resolvedBody(), input);
		} finally {
			closures.pop();
			definitionsBeingInferred.pop();
			inferenceContext = previousInferenceContext;
			for (int slot : definition.paramSlots()) {
				if (previous.containsKey(slot))
					variables.put(slot, previous.get(slot));
				else
					variables.remove(slot);
			}
		}
		functions.put(definition.slot(),
				List.of(TypeScheme.of(quantified, FunctionType.of(input, generalize(output, quantified.keySet()),
						parameters.toArray(FilterType[]::new)))));
		return NeverType.getInstance();
	}

	/**
	 * Types a call to a {@code def} by analysing its body against the input and arguments it was actually
	 * called with, which is what lets {@code def f: .a; {a: 1} | f} answer with the field's type rather than
	 * the {@code ANY} a single signature for every call site would have to give.
	 * <p>
	 * The generic pass in {@link #define} still stands behind it, for a definition reached some other way
	 * and for one that is never called at all.
	 */
	private Type localCall(AnalyzedExpression<?> call, String name, int slot, List<? extends AnalyzedExpression<?>> args, Type input) {
		// A call naming a parameter of the body being analysed: run the argument the caller passed, at the
		// input the body reached it with. jq gives a parameter no arguments of its own.
		Argument argument = arguments.get(slot);
		if (argument != null && args.isEmpty())
			return applyArgument(argument, input);
		ResolvedFunctionDefinition<?> definition = definitions.get(slot);
		if (definition == null || definition.paramSlots().size() != args.size())
			return applySchemes(call, name, args.size(), args, input, functions.getOrDefault(slot, dynamicFunction(args.size())));
		// A parameter is a filter, not a value: the body decides what input to run it on, so what travels
		// here is the argument itself together with the bindings it was written under.
		specializedDefinitions.add(slot);
		Map<Integer, Argument> filters = new HashMap<>();
		Map<Integer, Type> values = new HashMap<>();
		for (int i = 0; i < args.size(); i++) {
			// A `$`-prefixed parameter takes the argument's value, evaluated at this call's input; a plain
			// one takes the argument itself, for the body to run on whatever input it chooses.
			if (definition.paramNames().get(i).startsWith("$"))
				values.put(definition.paramSlots().get(i), infer(args.get(i), input));
			else
				filters.put(definition.paramSlots().get(i), capture(args.get(i)));
		}
		return inFrame(name, args.size(), call, /* opaqueBody */ false,
				() -> specialize(definition.resolvedBody(), input, filters, values,
						definitionClosures.getOrDefault(slot, Closures.EMPTY),
						() -> applySchemes(definition.resolvedBody(), name, args.size(), List.of(), input,
								functions.getOrDefault(slot, dynamicFunction(0)))));
	}

	/**
	 * Types a call to a function written in jq -- a builtin, or one a module brought along -- the same way
	 * as a call to a {@code def}: by analysing the body it is about to run. A filter parameter is bound to
	 * the caller's argument, which is what {@code JqFunctionCompiler}'s {@code boundFilter} does at
	 * runtime; a {@code $}-parameter is bound to the argument's value at this call's input.
	 */
	private Type boundJqCall(BoundJqFunctionCall<?> call, Type input) {
		List<FunctionParameter> parameters = call.parameters();
		List<? extends AnalyzedExpression<?>> args = call.arguments();
		if (parameters.size() != args.size())
			return opaqueCall(args, input);
		// A definition that states the types it accepts is checked against that statement instead of
		// against the body it is about to run, which is what lets it reject an input its body would only
		// have failed on by accident -- and what keeps a body the caller cannot change from reporting
		// anything of its own.
		if (!call.typeSchemes().isEmpty())
			return applySchemes(call, call.name(), args.size(), args, input, call.typeSchemes());
		Map<Integer, Argument> filters = new HashMap<>();
		Map<Integer, Type> values = new HashMap<>();
		for (int i = 0; i < parameters.size(); i++) {
			int slot = call.parameterBaseSlot() + i;
			if (parameters.get(i).kind() == FunctionParameter.Kind.FILTER)
				filters.put(slot, capture(args.get(i)));
			else
				values.put(slot, infer(args.get(i), input));
		}
		// A jq-source body is self-contained: its parameters are all it reads from outside itself.
		return inFrame(call.name(), args.size(), call, /* opaqueBody */ true,
				() -> specialize(call.body(), input, filters, values, Closures.EMPTY, () -> AnyType.getInstance()));
	}

	/**
	 * Wraps an argument expression together with the scope it was written in. An argument that is itself
	 * just one of the body's own parameters -- what {@code def w(f): ... w(f) ...} passes on every round --
	 * travels on as the argument already bound to it, rather than as a fresh wrapper around it. Otherwise
	 * each round of a recursion would produce an argument no earlier round could be recognised as, and the
	 * memo would never see the same call twice.
	 */
	private Argument capture(AnalyzedExpression<?> expression) {
		AnalyzedExpression<?> unwrapped = unwrap(expression);
		if (unwrapped instanceof ResolvedLocalFunctionAccess<?> call && call.args().isEmpty()) {
			Argument passedThrough = arguments.get(call.slot());
			if (passedThrough != null)
				return passedThrough;
		}
		// Kept beside the argument rather than inside it: Specialization keys on Argument, and a trace in
		// that key would split the memo by where a body was reached from, analysing it again -- and saying
		// the same thing again -- for every call site that differs in nothing else.
		argumentTraces.putIfAbsent(expression, List.copyOf(frames));
		return new Argument(expression, Map.copyOf(variables), Map.copyOf(arguments));
	}

	/**
	 * Analyses {@code body} at {@code input} with its parameters bound, memoizing on everything that
	 * analysis depends on so a body reached through many call sites of the same shape is analysed once.
	 * <p>
	 * A body that calls back into itself is re-analysed with the previous round's answer standing in for
	 * the recursive call, until that answer stops changing. A recursion whose result keeps growing falls
	 * back to {@code fallback}, because a wrong answer here would reject a query the runtime handles.
	 */
	private Type specialize(AnalyzedExpression<?> body, Type input, Map<Integer, Argument> filters,
							Map<Integer, Type> values, Closures bodyClosures, Supplier<Type> fallback) {
		Specialization key = new Specialization(body, input, Map.copyOf(filters), Map.copyOf(values), bodyClosures);
		Type memoized = specializations.get(key);
		if (memoized != null)
			return memoized;

		Type assumed = activeDefinitions.get(key);
		if (assumed != null) {
			recursiveDefinitions.add(key);
			return assumed;
		}
		if (activeDefinitions.size() >= MAX_CALL_DEPTH)
			return fallback.get();

		@Var Type result = NeverType.getInstance();
		for (int i = 0; i < FIXED_POINT_ITERATIONS; i++) {
			recursiveDefinitions.remove(key);
			activeDefinitions.put(key, result);
			Type next;
			try {
				next = runBody(body, input, filters, values, bodyClosures);
			} finally {
				activeDefinitions.remove(key);
			}
			boolean recursive = recursiveDefinitions.remove(key);
			if (!recursive || TypeEquivalence.isEqualType(next, result)) {
				specializations.put(key, next);
				return next;
			}
			result = StructuralWidening.fold(result, StructuralWidening.join(result, next), () -> fresh("Call"));
		}
		Type widened = fallback.get();
		specializations.put(key, widened);
		return widened;
	}

	private Type runBody(AnalyzedExpression<?> body, Type input, Map<Integer, Argument> filters,
						 Map<Integer, Type> values, Closures bodyClosures) {
		// A body sees its own parameters and nothing else of the caller's scope -- what it did capture
		// reaches it as a ResolvedCaptured*Access, typed from `closures` instead. Installing exactly the
		// parameters, rather than adding them on top of what the caller had, is also what makes the memo
		// sound: the key says nothing about the caller's bindings, so nothing may depend on them.
		Map<Integer, Argument> previousArguments = new HashMap<>(arguments);
		Map<Integer, Type> previousVariables = new HashMap<>(variables);
		arguments.clear();
		arguments.putAll(bodyClosures.capturedArguments());
		arguments.putAll(filters);
		variables.clear();
		variables.putAll(values);
		closures.push(bodyClosures);
		try {
			return infer(body, input);
		} finally {
			closures.pop();
			arguments.clear();
			arguments.putAll(previousArguments);
			variables.clear();
			variables.putAll(previousVariables);
		}
	}

	/**
	 * Evaluates an argument at the input the body reached it with, under the bindings that were in force
	 * where the argument was written rather than the ones in force inside the body.
	 */
	private Type applyArgument(Argument argument, Type input) {
		Map<Integer, Type> callerVariables = new HashMap<>(variables);
		Map<Integer, Argument> callerArguments = new HashMap<>(arguments);
		List<Frame> callerFrames = List.copyOf(frames);
		variables.clear();
		variables.putAll(argument.variables());
		arguments.clear();
		arguments.putAll(argument.arguments());
		installFrames(argumentTraces.getOrDefault(argument.expression(), List.of()));
		try {
			return infer(argument.expression(), input);
		} finally {
			installFrames(callerFrames);
			variables.clear();
			variables.putAll(callerVariables);
			arguments.clear();
			arguments.putAll(callerArguments);
		}
	}

	/**
	 * Returns a variable no other definition can name. Variables compare by name, so two definitions
	 * generating the same name would denote one variable, and a scheme quantifying it would capture
	 * the other definition's.
	 */
	private TypeVariable fresh(String name) {
		return TypeVariable.of(name + "_" + ++freshVariables);
	}

	private Type conditional(AnalyzedExpression<?> expression, Type input) {
		List<AnalyzedExpression<?>> children = children(expression);
		List<Type> outputs = new ArrayList<>();
		@Var Type remaining = input;
		for (int i = 0; i + 1 < children.size() - 1; i += 2) {
			AnalyzedExpression<?> condition = children.get(i);
			infer(condition, remaining);
			Narrowing narrowing = narrowing(condition, remaining);
			if (narrowing.whenTrue != NeverType.getInstance())
				outputs.add(infer(children.get(i + 1), narrowing.whenTrue));
			else
				warn(children.get(i + 1), "Branch is unreachable for input type " + remaining);
			remaining = narrowing.whenFalse;
		}
		AnalyzedExpression<?> last = children.get(children.size() - 1);
		if (remaining != NeverType.getInstance())
			outputs.add(infer(last, remaining));
		else if (!(unwrap(last) instanceof ThisObject<?>))
			// An `if` written without `else` compiles to `else .`, which is not a branch anyone wrote and so
			// not a branch worth warning about.
			warn(last, "Branch is unreachable for the narrowed input type");
		return UnionType.of(outputs);
	}

	private static Narrowing narrowing(AnalyzedExpression<?> condition, Type input) {
		AnalyzedExpression<?> unwrapped = unwrap(condition);
		if (!(unwrapped instanceof CompareEqualTest<?> || unwrapped instanceof CompareNotEqualTest<?>))
			return new Narrowing(input, input);
		AbstractBinaryOperatorExpression<?> comparison = (AbstractBinaryOperatorExpression<?>) unwrapped;
		@Var @Nullable Type selected = selectedType(comparison.lhs(), comparison.rhs());
		if (selected == null)
			selected = selectedType(comparison.rhs(), comparison.lhs());
		if (selected == null)
			return new Narrowing(input, input);
		List<Type> yes = new ArrayList<>();
		List<Type> no = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(input)) {
			// A variable standing in for "whatever this definition is called with" is not a value type, so
			// nothing can be ruled in or out by asking what type it is.
			if (alternative instanceof TypeVariable)
				return new Narrowing(input, input);
			// ANY admits values of the tested type and values of every other, so it goes both ways -- and
			// the branch that runs knows more about its input than ANY.
			if (alternative instanceof AnyType) {
				yes.add(selected);
				no.add(alternative);
				continue;
			}
			(alternative.getClass() == selected.getClass() ? yes : no).add(alternative);
		}
		Narrowing result = new Narrowing(UnionType.of(yes), UnionType.of(no));
		return unwrapped instanceof CompareNotEqualTest<?>
				? new Narrowing(result.whenFalse, result.whenTrue) : result;
	}

	private static @Nullable Type selectedType(AnalyzedExpression<?> typeExpression,
											   AnalyzedExpression<?> literalExpression) {
		AnalyzedExpression<?> type = unwrap(typeExpression);
		String literal = stringLiteral(literalExpression);
		if (!(type instanceof UnboundFunctionCall<?> call) || literal == null)
			return null;
		return call.factory().getInputTypeRefinement(literal);
	}

	/**
	 * What an array answers at {@code position}, or at an unknown position when it is null. Out of range
	 * is null at runtime, so a position the array may not reach contributes one.
	 */
	private static Type element(ArrayType array, @Nullable Integer position) {
		if (position == null)
			return UnionType.of(array.elementType(), NullType.getInstance());
		@Var
		int index = position;
		if (index < 0) {
			// A negative index counts back from the end, which only an array of known length has.
			OptionalInt length = TypeRelations.exactLength(array);
			if (length.isEmpty() || length.getAsInt() + index < 0)
				return UnionType.of(array.elementType(), NullType.getInstance());
			index += length.getAsInt();
		}
		return TypeRelations.absentAsNull(TypeRelations.elementAt(array, index));
	}

	/**
	 * The value of an integer literal, or null when the expression is not one. A fractional literal is not
	 * one either: jq truncates it, and saying which position that lands on is not worth a second rule.
	 */
	static @Nullable Integer integerLiteral(AnalyzedExpression<?> expression) {
		@Var
		AnalyzedExpression<?> unwrapped = unwrap(expression);
		// `-1` is a negation of a literal rather than a literal, both in the source and in the tree.
		@Var
		boolean negated = false;
		if (unwrapped instanceof NegativeExpression<?>) {
			negated = true;
			unwrapped = unwrap(onlyChild(unwrapped));
		}
		if (!(unwrapped instanceof ValueLiteral<?> value))
			return null;
		@Var
		BigDecimal literal = value.numberLiteral();
		if (literal != null && negated)
			literal = literal.negate();
		if (literal == null || literal.stripTrailingZeros().scale() > 0)
			return null;
		try {
			return literal.intValueExact();
		} catch (ArithmeticException e) {
			// An index this far out is null whatever the array holds, but so is any index past the end.
			return null;
		}
	}

	static @Nullable String stringLiteral(AnalyzedExpression<?> expression) {
		AnalyzedExpression<?> unwrapped = unwrap(expression);
		return unwrapped instanceof ValueLiteral<?> value ? value.stringLiteral() : null;
	}

	static AnalyzedExpression<?> unwrap(AnalyzedExpression<?> expression) {
		@Var AnalyzedExpression<?> result = expression;
		while (result instanceof AbstractDelegatingExpression<?> delegating)
			result = delegating.inner();
		return result;
	}

	private Type tryCatch(AnalyzedExpression<?> expression, Type input) {
		List<AnalyzedExpression<?>> children = children(expression);
		boolean previous = guarded;
		guarded = true;
		Type attempted;
		try {
			attempted = infer(children.get(0), input);
		} finally {
			guarded = previous;
		}
		return children.size() == 1 ? attempted : UnionType.of(attempted, infer(children.get(1), AnyType.getInstance()));
	}

	private Type applyFilterSchemes(AnalyzedExpression<?> expression, Type input) {
		// A folded expression publishes no signature, so reaching one here would quietly type it ANY and let
		// whatever it replaced go unchecked. Folding runs after this, so arriving at one means that ordering
		// broke; say so rather than infer a type from a tree the query was not written as.
		if (expression instanceof FoldedConstantExpression<?> || expression instanceof FoldedErrorExpression<?>)
			throw new IllegalStateException("Constant folding ran before type checking: " + expression.getClass().getName());
		List<Type> outputs = new ArrayList<>();
		for (TypeScheme<FilterType> scheme : expression.getTypeSchemes()) {
			TypeMatcher matcher = new TypeMatcher(scheme);
			if (matcher.match(scheme.body().inputType(), input) && matcher.validateBounds())
				outputs.add(matcher.substitute(scheme.body().outputType()));
		}
		if (outputs.isEmpty())
			throw new TypeRelations.Problem("Expression does not accept input " + input);
		return UnionType.of(outputs);
	}

	private Type applySchemes(AnalyzedExpression<?> expression, String name, int callArity,
							  List<? extends AnalyzedExpression<?>> arguments,
							  Type input, List<TypeScheme<FunctionType>> schemes) {
		String function = name + "/" + callArity;
		List<MatchedOverload> matched = new ArrayList<>();
		@Var boolean hasMatchingArity = false;
		@Var boolean hasMatchingInput = false;
		@Var boolean onlySameArgumentMismatch = true;
		@Var @Nullable ArgumentMismatch commonMismatch = null;
		for (TypeScheme<FunctionType> scheme : schemes) {
			TypeMatcher matcher = new TypeMatcher(scheme);
			FunctionType type = scheme.body();
			if (type.parameterTypes().size() != arguments.size())
				continue;
			hasMatchingArity = true;
			if (!matcher.match(type.returnType().inputType(), input))
				continue;
			hasMatchingInput = true;
			@Var @Nullable ArgumentMismatch mismatch = null;
			for (int i = 0; i < arguments.size(); i++) {
				FilterType parameter = type.parameterTypes().get(i);
				Type argument = infer(arguments.get(i), matcher.substitute(parameter.inputType()));
				if (!matcher.match(parameter.outputType(), argument)) {
					mismatch = new ArgumentMismatch(i, matcher.substitute(parameter.outputType()), argument, arguments.get(i));
					break;
				}
			}
			if (mismatch != null) {
				if (commonMismatch == null)
					commonMismatch = mismatch;
				else if (!commonMismatch.sameTypesAs(mismatch))
					onlySameArgumentMismatch = false;
			} else if (matcher.validateBounds()) {
				List<FilterType> effectiveParameters = new ArrayList<>(type.parameterTypes().size());
				for (FilterType parameter : type.parameterTypes()) {
					effectiveParameters.add(FilterType.of(
							matcher.substitute(parameter.inputType()),
							matcher.substitute(parameter.outputType())));
				}
				matched.add(new MatchedOverload(
						matcher.substitute(type.returnType().inputType()),
						effectiveParameters,
						matcher.substitute(type.returnType().outputType())));
			} else {
				onlySameArgumentMismatch = false;
			}
		}
		if (matched.isEmpty()) {
			if (hasMatchingInput && onlySameArgumentMismatch && commonMismatch != null) {
				String message = "Argument " + (commonMismatch.index() + 1) + " of " + function + " has type "
						+ commonMismatch.actual() + "; expected " + commonMismatch.expected();
				@Nullable SourceLocation location = sourceLocation(commonMismatch.expression());
				report(withAcceptedTypes(message, name, schemes), location != null ? location : sourceLocation(expression), function);
				return guarded ? NeverType.getInstance() : AnyType.getInstance();
			}
			String message = hasMatchingArity && !hasMatchingInput
					? "No overload of " + function + " accepts input " + input
					: "No overload of " + function + " matches this call with input " + input;
			throw new TypeRelations.Problem(withAcceptedTypes(message, name, schemes), function);
		}
		List<Type> outputs = new ArrayList<>(matched.size());
		for (MatchedOverload candidate : matched) {
			@Var boolean subsumed = false;
			for (MatchedOverload other : matched) {
				if (candidate != other && isStrictlyMoreSpecific(other, candidate)) {
					subsumed = true;
					break;
				}
			}
			if (!subsumed)
				outputs.add(candidate.output());
		}
		return UnionType.of(outputs);
	}

	private record MatchedOverload(Type effectiveInput, List<FilterType> effectiveParameters, Type output) {
	}

	private static boolean isStrictlyMoreSpecific(MatchedOverload narrower, MatchedOverload wider) {
		if (!TypeMatcher.isSubtype(narrower.effectiveInput(), wider.effectiveInput()))
			return false;
		for (int i = 0; i < narrower.effectiveParameters().size(); i++) {
			FilterType nParam = narrower.effectiveParameters().get(i);
			FilterType wParam = wider.effectiveParameters().get(i);
			if (!TypeMatcher.isSubtype(wParam.inputType(), nParam.inputType()))
				return false;
			if (!TypeMatcher.isSubtype(nParam.outputType(), wParam.outputType()))
				return false;
		}
		if (!TypeMatcher.isSubtype(wider.effectiveInput(), narrower.effectiveInput()))
			return true;
		for (int i = 0; i < narrower.effectiveParameters().size(); i++) {
			FilterType nParam = narrower.effectiveParameters().get(i);
			FilterType wParam = wider.effectiveParameters().get(i);
			if (!TypeMatcher.isSubtype(nParam.inputType(), wParam.inputType()))
				return true;
			if (!TypeMatcher.isSubtype(wParam.outputType(), nParam.outputType()))
				return true;
		}
		return false;
	}

	private static String withAcceptedTypes(String message, String name, List<TypeScheme<FunctionType>> schemes) {
		StringBuilder result = new StringBuilder(message).append("\nAccepted types:");
		if (schemes.isEmpty())
			return result.append("\n  (none published)").toString();
		for (TypeScheme<FunctionType> scheme : schemes) {
			FunctionType type = scheme.body();
			result.append("\n  ");
			if (!scheme.typeVariables().isEmpty()) {
				result.append('<');
				@Var boolean first = true;
				for (Map.Entry<TypeVariable, Type> entry : scheme.typeVariables().entrySet()) {
					if (!first)
						result.append(", ");
					first = false;
					result.append(entry.getKey().name());
					Type bound = entry.getValue();
					if (bound != AnyType.getInstance())
						result.append(": ").append(bound);
				}
				result.append("> ");
			}
			result.append("Input: ").append(type.returnType().inputType()).append(" -> ").append(name).append('(');
			for (int i = 0; i < type.parameterTypes().size(); i++) {
				if (i > 0)
					result.append("; ");
				result.append(type.parameterTypes().get(i));
			}
			result.append(") -> Output: ").append(type.returnType().outputType());
		}
		return result.toString();
	}

	private Type opaqueCall(List<? extends AnalyzedExpression<?>> arguments, Type input) {
		for (AnalyzedExpression<?> argument : arguments)
			infer(argument, input);
		return AnyType.getInstance();
	}

	/**
	 * {@code LHS = RHS} emits once per RHS value, each being the input with every selected position set to
	 * that value. An RHS that emits nothing emits no assignment either.
	 */
	private Type assignment(Assignment<?> assignment, Type input) {
		Type replacement = infer(assignment.rhs(), input);
		if (replacement == NeverType.getInstance())
			return NeverType.getInstance();
		return mutate(input, assignment.lhs(), replacement);
	}

	/**
	 * {@code LHS |= RHS} runs RHS on the selected value. An RHS that emits nothing has no defined answer --
	 * {@code UpdateAssignment} raises {@code JsonQueryUndefinedBehaviorException} rather than deleting the
	 * path the way jq 1.6 and later do.
	 */
	private Type updateAssignment(UpdateAssignment<?> assignment, Type input) {
		Type selected = infer(assignment.lhs(), input);
		Type replacement = infer(assignment.rhs(), selected);
		if (replacement == NeverType.getInstance())
			throw new TypeRelations.Problem("`|= empty` is undefined");
		return mutate(input, assignment.lhs(), replacement);
	}

	/**
	 * {@code LHS op= RHS} evaluates RHS against the original input, not the selected value, and combines it
	 * with the selected value using {@code op}'s own rule.
	 */
	private Type complexAssignment(AbstractComplexAssignment<?> assignment, Type input) {
		Type selected = infer(assignment.lhs(), input);
		Type right = infer(assignment.rhs(), input);
		if (right == NeverType.getInstance())
			return NeverType.getInstance();
		Type replacement;
		if (assignment instanceof ComplexPlusAssignment<?>)
			replacement = TypeRelations.plus(selected, right);
		else if (assignment instanceof ComplexMinusAssignment<?>)
			replacement = TypeRelations.minus(selected, right);
		else if (assignment instanceof ComplexMultiplyAssignment<?>)
			replacement = TypeRelations.multiply(selected, right);
		else if (assignment instanceof ComplexDivideAssignment<?>)
			replacement = TypeRelations.divide(selected, right);
		else if (assignment instanceof ComplexModuloAssignment<?>)
			replacement = TypeRelations.modulo(selected, right);
		else if (assignment instanceof ComplexAlternativeAssignment<?>)
			replacement = UnionType.of(TypeRelations.withoutNull(selected), right);
		else
			throw new IllegalStateException("Missing type rule for assignment " + assignment.getClass().getName());
		return mutate(input, assignment.lhs(), replacement);
	}

	private Type mutate(Type input, AnalyzedExpression<?> selector, Type replacement) {
		return assignments.apply(input, selector, replacement);
	}

	private Type binary(AnalyzedExpression<?> expression, Type input, BinaryRule rule) {
		List<Type> operands = inferBinaryChildren(expression, input);
		return rule.apply(operands.get(0), operands.get(1));
	}

	private List<Type> inferBinaryChildren(AnalyzedExpression<?> expression, Type input) {
		AbstractBinaryOperatorExpression<?> binary = (AbstractBinaryOperatorExpression<?>) expression;
		return List.of(infer(binary.lhs(), input), infer(binary.rhs(), input));
	}

	static List<AnalyzedExpression<?>> children(AnalyzedExpression<?> expression) {
		List<AnalyzedExpression<?>> children = new ArrayList<>();
		if (expression instanceof RewritableExpression<?> rewritable)
			rewritable.rewriteChildren(child -> {
				children.add(child);
				return child;
			});
		return children;
	}

	private static AnalyzedExpression<?> onlyChild(AnalyzedExpression<?> expression) {
		List<AnalyzedExpression<?>> children = children(expression);
		if (children.size() != 1)
			throw new IllegalStateException(expression.getClass().getSimpleName() + " must have one child");
		return children.get(0);
	}

	private static List<TypeScheme<FunctionType>> dynamicFunction(int arguments) {
		return List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), AnyType.getInstance(),
				Collections.nCopies(arguments, FilterType.of(AnyType.getInstance(), AnyType.getInstance())).toArray(FilterType[]::new))));
	}

	static void require(Type expected, Type actual, String message) {
		if (!TypeMatcher.accepts(expected, actual))
			throw new TypeRelations.Problem(message);
	}

	private Type guarded(AnalyzedExpression<?> expression, TypeSupplier supplier, boolean suppress) {
		if (!suppress)
			return supplier.get();
		boolean previous = guarded;
		guarded = true;
		try {
			return supplier.get();
		} catch (TypeRelations.Problem problem) {
			report(expression, message(problem), problem.callee());
			return NeverType.getInstance();
		} finally {
			guarded = previous;
		}
	}

	private Map<Integer, Type> installVariables(Map<Integer, Type> bindings) {
		Map<Integer, Type> previous = new HashMap<>();
		for (Map.Entry<Integer, Type> binding : bindings.entrySet()) {
			if (variables.containsKey(binding.getKey()))
				previous.put(binding.getKey(), variables.get(binding.getKey()));
			variables.put(binding.getKey(), binding.getValue());
		}
		return previous;
	}

	private void restoreVariables(Set<Integer> slots, Map<Integer, Type> previous) {
		for (int slot : slots) {
			if (previous.containsKey(slot))
				variables.put(slot, previous.get(slot));
			else
				variables.remove(slot);
		}
	}

	private static String message(TypeRelations.Problem problem) {
		return problem.getMessage() != null ? problem.getMessage() : problem.toString();
	}

	private void report(AnalyzedExpression<?> expression, String message) {
		report(expression, message, null);
	}

	private void report(AnalyzedExpression<?> expression, String message, @Nullable String callee) {
		report(message, sourceLocation(expression), callee);
	}

	private void report(String message, @Nullable SourceLocation location, @Nullable String callee) {
		Diagnostic.Severity severity = guarded || inferenceContext || mode == TypeCheckMode.WARN
				? Diagnostic.Severity.WARNING : Diagnostic.Severity.ERROR;
		if (severity == Diagnostic.Severity.ERROR)
			errors++;
		emit(Diagnostic.of(severity, withCallTrace(message, callee), locate(location)));
	}

	private @Nullable SourceLocation sourceLocation(AnalyzedExpression<?> expression) {
		@Var AnalyzedExpression<?> current = expression;
		while (true) {
			SourceLocation location = locations.get(current);
			if (location != null)
				return location;
			if (!(current instanceof AbstractDelegatingExpression<?> delegating))
				return null;
			current = delegating.inner();
		}
	}

	private void warn(AnalyzedExpression<?> expression, String message) {
		emit(Diagnostic.warning(withCallTrace(message, null), locate(sourceLocation(expression))));
	}

	/**
	 * Analyses a callee's body with the call itself on the trace, so a diagnostic the body raises says which
	 * call led to it.
	 */
	private Type inFrame(String name, int arity, AnalyzedExpression<?> call, boolean opaqueBody, TypeSupplier body) {
		// A call written inside a body the caller cannot see contributes no location: the locations map
		// either has no entry for it -- a library a loader brought along compiles against a context of its
		// own -- or has one measured against that library's source text rather than the query's.
		frames.push(new Frame(name, arity, opaqueFrames == 0 ? sourceLocation(call) : null, opaqueBody));
		if (opaqueBody)
			opaqueFrames++;
		try {
			return body.get();
		} finally {
			if (opaqueBody)
				opaqueFrames--;
			frames.pop();
		}
	}

	private void installFrames(List<Frame> trace) {
		frames.clear();
		@Var int opaque = 0;
		for (Frame frame : trace) {
			frames.addLast(frame);
			if (frame.opaqueBody())
				opaque++;
		}
		opaqueFrames = opaque;
	}

	/**
	 * Where to say a diagnostic is about. Inside a body the caller cannot see, an expression's own location
	 * is either missing or points at source the caller never wrote, so the innermost call the query itself
	 * made is the only place worth naming.
	 */
	private @Nullable SourceLocation locate(@Nullable SourceLocation expressionLocation) {
		if (opaqueFrames == 0)
			return expressionLocation;
		for (Frame frame : frames) {
			if (frame.callSite() != null)
				return frame.callSite();
		}
		return null;
	}

	/**
	 * Appends the calls between the query and this diagnostic, innermost first. Nothing is appended for a
	 * diagnostic the query raised directly: the expression it is reported at is already the whole story, and
	 * for a call the message names the function itself.
	 */
	private String withCallTrace(String message, @Nullable String callee) {
		if (frames.isEmpty())
			return message;
		List<String> trace = new ArrayList<>();
		if (callee != null)
			trace.add(callee);
		for (Frame frame : frames) {
			String name = frame.name() + "/" + frame.arity();
			// A body that reaches itself names itself once: that it recurses is one fact, not sixty.
			if (trace.isEmpty() || !trace.get(trace.size() - 1).equals(name))
				trace.add(name);
		}
		StringBuilder result = new StringBuilder(message);
		for (String name : trace.subList(0, Math.min(trace.size(), MAX_TRACE_FRAMES)))
			result.append("\n  in ").append(name);
		if (trace.size() > MAX_TRACE_FRAMES)
			result.append("\n  ... ").append(trace.size() - MAX_TRACE_FRAMES).append(" more");
		return result.toString();
	}

	private void emit(Diagnostic diagnostic) {
		Integer definition = definitionsBeingInferred.peek();
		if (definition != null) {
			deferredDiagnostics.computeIfAbsent(definition, slot -> new ArrayList<>()).add(diagnostic);
			return;
		}
		if (listener != null)
			listener.report(diagnostic);
	}

	private void reportUncalledDefinitions() {
		if (listener == null)
			return;
		for (Map.Entry<Integer, List<Diagnostic>> deferred : deferredDiagnostics.entrySet()) {
			if (specializedDefinitions.contains(deferred.getKey()))
				continue;
			for (Diagnostic diagnostic : deferred.getValue())
				listener.report(diagnostic);
		}
	}

	@FunctionalInterface
	private interface TypeSupplier {
		Type get();
	}

	@FunctionalInterface
	private interface BinaryRule {
		Type apply(Type left, Type right);
	}

	@FunctionalInterface
	private interface Iterate {
		Iteration run(Type accumulator);
	}

	/** What one pass of a loop body leaves the accumulator at, and what that pass emits. */
	private record Iteration(Type updated, Type emitted) {
	}

	/** Where an accumulator settled, and everything the loop emitted on the way. */
	private record Accumulation(Type output, Type emitted) {
	}

	private record Narrowing(Type whenTrue, Type whenFalse) {
	}

	/**
	 * One call the analysis is inside. {@code callSite} is where the call was written, when that is somewhere
	 * the caller can see; {@code opaqueBody} is true when the body it entered is source the caller did not
	 * write -- a function a library brought along -- and so cannot be pointed at.
	 */
	private record Frame(String name, int arity, @Nullable SourceLocation callSite, boolean opaqueBody) {
	}

	private record ArgumentMismatch(int index, Type expected, Type actual, AnalyzedExpression<?> expression) {
		boolean sameTypesAs(ArgumentMismatch other) {
			return index == other.index && expected.equals(other.expected) && actual.equals(other.actual);
		}
	}

	/**
	 * What a definition's closure holds, worked out where the definition was written: the types of the
	 * variables it captured, the definitions its captured functions name, and -- for a captured function
	 * that is a filter parameter of the enclosing def rather than a def of its own -- the argument bound to
	 * it. {@code recurse} is the standard shape: {@code def recurse(f): def r: ., (f | r); r;}, where
	 * {@code r} reaches {@code f} only through its closure.
	 */
	private record Closures(Map<Integer, Type> variables, Map<Integer, Integer> functions,
							Map<Integer, Argument> capturedArguments) {
		static final Closures EMPTY = new Closures(Map.of(), Map.of(), Map.of());
	}

	/**
	 * An argument a call bound to a parameter slot, with the scope it was written in. {@code map(.a)} runs
	 * {@code .a} inside map's body, but the user wrote it, so it is inferred under the caller's bindings.
	 */
	private record Argument(AnalyzedExpression<?> expression, Map<Integer, Type> variables,
							Map<Integer, Argument> arguments) {
	}

	/**
	 * Everything a body's analysis depends on, and so everything its result may be reused for: the body
	 * itself, the input, the arguments bound to its parameters, and what its closure holds. Leaving any of
	 * them out would hand one call site the answer worked out for another.
	 * <p>
	 * Expressions compare by identity, which is what {@link Object#equals} gives here: an argument is the
	 * same argument when it is the same expression, written at the same place, and the scope it carries
	 * tells the two analyses of that one place apart.
	 * <p>
	 * The memo also fixes which call site a body's diagnostics are reported at: the first one to reach it.
	 * A second call site of the same shape is answered from here and says nothing of its own, which is the
	 * same body reported once rather than once per caller.
	 */
	private record Specialization(AnalyzedExpression<?> body, Type input, Map<Integer, Argument> filters,
								  Map<Integer, Type> values, Closures closures) {
	}
}
