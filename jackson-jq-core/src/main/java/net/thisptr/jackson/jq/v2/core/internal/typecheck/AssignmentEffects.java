package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.Comma;
import net.thisptr.jackson.jq.v2.core.internal.tree.Conditional;
import net.thisptr.jackson.jq.v2.core.internal.tree.PipedQuery;
import net.thisptr.jackson.jq.v2.core.internal.tree.RecursionOperator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.VariableBinding;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AlternativeOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.IdentifierFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.StringFieldAccess;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * What an assignment does to the type of the value it assigns into.
 * <p>
 * An assignment applies its left-hand side to {@code .} as a <em>path</em> and writes into every path it
 * produces ({@code Assignment}, {@code AbstractComplexAssignment} and {@code UpdateAssignment} all drive
 * {@code PathOperations.mutate} that way), so the result is the input with those positions replaced. This
 * walks the same selector forms the path machinery recognises and applies the replacement to the type at
 * each one, leaving everything the selector does not reach alone.
 * <p>
 * A selector this cannot follow -- a call whose body is not visible here, {@code getpath} with a path only
 * known at runtime -- is not a reason to reject the query, so it widens instead. How far it widens turns on
 * one checkable fact: a selector whose selected value has the type of the input selects <em>this</em>
 * position, so the position ends up as either the old value or the new one. Anything else may be writing
 * somewhere below, and only widening the descendants is honest.
 */
final class AssignmentEffects {
	private final TypeCheck check;
	// One inference per selector node per assignment. Without it a chain re-infers each prefix on the way
	// down and again on the way up, which reports the same diagnostic once per level.
	private final Map<AnalyzedExpression<?>, Type> selected = new IdentityHashMap<>();

	AssignmentEffects(TypeCheck check) {
		this.check = check;
	}

	/**
	 * The type of {@code input} once every position {@code selector} selects holds {@code replacement}.
	 */
	Type apply(Type input, AnalyzedExpression<?> selector, Type replacement) {
		AnalyzedExpression<?> unwrapped = TypeCheck.unwrap(selector);

		if (unwrapped instanceof ThisObject<?>)
			return replacement;

		if (unwrapped instanceof IdentifierFieldAccess<?> field)
			return apply(input, field.target(), setField(select(field.target(), input), field.field(), replacement));

		if (unwrapped instanceof StringFieldAccess<?> field) {
			Type target = select(field.target(), input);
			@Nullable String key = TypeCheck.stringLiteral(field.key());
			return apply(input, field.target(), key != null
					? setField(target, key, replacement)
					: setUnknownField(target, replacement));
		}

		if (unwrapped instanceof BracketFieldAccess<?> bracket) {
			Type target = select(bracket.target(), input);
			return apply(input, bracket.target(), bracket.isRange()
					? setSlice(target, replacement)
					: setIndex(bracket, target, input, replacement));
		}

		if (unwrapped instanceof BracketExtractFieldAccess<?> extract)
			return apply(input, extract.target(), setEveryMember(select(extract.target(), input), replacement));

		if (unwrapped instanceof PipedQuery<?>) {
			List<AnalyzedExpression<?>> parts = TypeCheck.children(unwrapped);
			Type intermediate = select(parts.get(0), input);
			return apply(input, parts.get(0), apply(intermediate, parts.get(1), replacement));
		}

		// `(.a, .b) = v` writes both, each into the result of the one before it.
		if (unwrapped instanceof Comma<?>) {
			@Var Type result = input;
			for (AnalyzedExpression<?> child : TypeCheck.children(unwrapped))
				result = apply(result, child, replacement);
			return result;
		}

		// Only one branch runs, so the result is one of their effects -- not all of them applied in turn.
		if (unwrapped instanceof Conditional<?> || unwrapped instanceof AlternativeOperatorExpression<?>) {
			List<Type> outputs = new ArrayList<>();
			for (AnalyzedExpression<?> branch : selectorBranches(unwrapped))
				outputs.add(apply(input, branch, replacement));
			return UnionType.of(outputs);
		}

		// A guarded selector that fails selects nothing, which leaves the input as it was.
		if (unwrapped instanceof TryCatch<?>) {
			List<AnalyzedExpression<?>> children = TypeCheck.children(unwrapped);
			return UnionType.of(input, apply(input, children.get(0), replacement));
		}

		// `. as $x | .a` binds first, then selects: the body is the selector, in the pattern's scope.
		if (unwrapped instanceof VariableBinding<?> binding)
			return check.withPatternBindings(binding.matcher(), select(binding.value(), input),
					() -> apply(input, binding.body(), replacement));

		// `..` reaches every position below the input, so every one of them may hold the replacement.
		if (unwrapped instanceof RecursionOperator<?>)
			return widenDescendants(input, replacement);

		return widenUnrepresentable(input, selector, replacement);
	}

	/**
	 * Applies a selector this does not model. Its children are still inferred, so their own type errors are
	 * reported rather than swallowed along with the selector.
	 */
	private Type widenUnrepresentable(Type input, AnalyzedExpression<?> selector, Type replacement) {
		Type value = select(selector, input);
		if (TypeEquivalence.isEqualType(value, input))
			return UnionType.of(input, replacement);
		return widenDescendants(input, replacement);
	}

	/**
	 * The input with the replacement admitted at this position and at every position below it.
	 */
	private static Type widenDescendants(Type input, Type replacement) {
		List<Type> outputs = new ArrayList<>();
		outputs.add(replacement);
		for (Type alternative : TypeRelations.alternatives(input)) {
			if (alternative instanceof ArrayType array) {
				outputs.add(ArrayType.of(widenDescendants(array.elementType(), replacement)));
			} else if (alternative instanceof ObjectType object) {
				Map<String, Type> fields = new TreeMap<>();
				for (Map.Entry<String, Type> field : object.fields().entrySet())
					fields.put(field.getKey(), widenDescendants(field.getValue(), replacement));
				outputs.add(ObjectType.of(fields, object.isClosed() ? NeverType.getInstance()
						: widenDescendants(object.additionalFieldType(), replacement)));
			} else {
				outputs.add(alternative);
			}
		}
		return UnionType.of(outputs);
	}

	private Type select(AnalyzedExpression<?> expression, Type input) {
		Type cached = selected.get(expression);
		if (cached != null)
			return cached;
		Type value = check.infer(expression, input);
		selected.put(expression, value);
		return value;
	}

	private static List<AnalyzedExpression<?>> selectorBranches(AnalyzedExpression<?> expression) {
		List<AnalyzedExpression<?>> children = TypeCheck.children(expression);
		if (!(expression instanceof Conditional<?>))
			return children;
		// if c1 then b1 elif c2 then b2 else b3 end compiles to c1, b1, c2, b2, b3: the conditions are not
		// selectors, only the bodies are.
		List<AnalyzedExpression<?>> branches = new ArrayList<>();
		for (int i = 1; i < children.size(); i += 2)
			branches.add(children.get(i));
		branches.add(children.get(children.size() - 1));
		return branches;
	}

	private Type setIndex(BracketFieldAccess<?> bracket, Type target, Type input, Type replacement) {
		@Nullable String key = TypeCheck.stringLiteral(bracket.startExpr());
		if (key != null)
			return setField(target, key, replacement);
		Type index = select(bracket.startExpr(), input);
		if (index instanceof StringType)
			return setUnknownField(target, replacement);
		if (index instanceof NumericType)
			return setElement(target, replacement);
		return setUnknownField(setElement(target, replacement), replacement);
	}

	/**
	 * Writing past the end of an array fills the gap with nulls, so an indexed write admits null as well as
	 * the replacement.
	 */
	private static Type setElement(Type target, Type replacement) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType array)
				outputs.add(ArrayType.of(UnionType.of(array.elementType(), replacement, NullType.getInstance())));
			else if (alternative instanceof NullType)
				outputs.add(ArrayType.of(UnionType.of(replacement, NullType.getInstance())));
			else
				throw new TypeRelations.Problem("Cannot update " + alternative + " at a numeric index");
		}
		return UnionType.of(outputs);
	}

	private static Type setSlice(Type target, Type replacement) {
		Type replacementElement = sliceReplacementElement(replacement);
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType array)
				outputs.add(ArrayType.of(UnionType.of(array.elementType(), replacementElement)));
			else if (alternative instanceof NullType)
				outputs.add(ArrayType.of(replacementElement));
			else if (alternative instanceof StringType)
				throw new TypeRelations.Problem("Cannot update string slices");
			else
				throw new TypeRelations.Problem("Cannot update " + alternative + " with a slice");
		}
		return UnionType.of(outputs);
	}

	private static Type sliceReplacementElement(Type replacement) {
		List<Type> elements = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(replacement)) {
			if (alternative instanceof AnyType)
				elements.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType array)
				elements.add(array.elementType());
			else
				throw new TypeRelations.Problem("A slice of an array can only be assigned another array, not " + alternative);
		}
		return UnionType.of(elements);
	}

	static Type setField(Type target, String name, Type replacement) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ObjectType object) {
				Map<String, Type> fields = new TreeMap<>(object.fields());
				fields.put(name, replacement);
				outputs.add(ObjectType.of(fields, object.additionalFieldType()));
			} else if (alternative instanceof NullType)
				outputs.add(ObjectType.of(name, replacement));
			else
				throw new TypeRelations.Problem("Cannot update " + alternative + " with a string key");
		}
		return UnionType.of(outputs);
	}

	/**
	 * A write through a key only known at runtime: any one declared field may be the one written, and an
	 * undeclared one may be created, so the object stops being closed.
	 */
	private static Type setUnknownField(Type target, Type replacement) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ObjectType object) {
				Map<String, Type> fields = new TreeMap<>();
				for (Map.Entry<String, Type> field : object.fields().entrySet())
					fields.put(field.getKey(), UnionType.of(field.getValue(), replacement));
				outputs.add(ObjectType.of(fields, UnionType.of(object.additionalFieldType(), replacement)));
			} else if (alternative instanceof NullType)
				outputs.add(ObjectType.of(Map.of(), replacement));
			else
				throw new TypeRelations.Problem("Cannot update " + alternative + " with a string key");
		}
		return UnionType.of(outputs);
	}

	/**
	 * {@code .[] = v}: every member is written, and a declared field's own type is replaced outright rather
	 * than widened, because every one of them is reached.
	 */
	private static Type setEveryMember(Type target, Type replacement) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : TypeRelations.alternatives(target)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType)
				outputs.add(ArrayType.of(replacement));
			else if (alternative instanceof ObjectType object) {
				Map<String, Type> fields = new TreeMap<>();
				for (String name : object.fields().keySet())
					fields.put(name, replacement);
				outputs.add(ObjectType.of(fields, object.isClosed() ? NeverType.getInstance() : replacement));
			} else
				throw new TypeRelations.Problem("Cannot iterate over " + alternative);
		}
		return UnionType.of(outputs);
	}
}
