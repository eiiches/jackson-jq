package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.OptimizationOptions;
import net.thisptr.jackson.jq.v2.core.internal.compile.opt.FoldPlanner;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

public class CompileContext {
	private static class ScopeFrame {
		final boolean isFunctionBoundary;
		// True only for a JqFunctionCompiler-inlined library-function scope: it is still a function
		// boundary (own params/nested defs get ordinary closure-capture treatment), but a name not found
		// within it (or within a real function scope nested inside it) must never resolve by walking
		// further outward -- unlike an ordinary def, whose whole point is that the walk *does* cross it.
		final boolean isolatesLexicalScope;
		// True only for a JqFunctionCompiler-inlined library-function scope: unlike an ordinary def (which
		// becomes its own independent runtime frame, so its slot count never affects the caller's), this
		// scope shares its enclosing scope's physical frame, so its final nextSlot high-water mark must be
		// folded back into the parent on pop -- the same as an ordinary (non-function-boundary) local scope.
		final boolean foldsSlotCountOnPop;
		final Set<String> variables = new HashSet<>();
		final Set<FunctionSignature> functions = new HashSet<>();
		final Map<String, Integer> variableSlots = new HashMap<>();
		final Map<FunctionSignature, Integer> functionSlots = new HashMap<>();
		final Map<String, BoundArgumentInfo> variableBoundArguments = new HashMap<>();
		final Map<FunctionSignature, BoundArgumentInfo> functionBoundArguments = new HashMap<>();
		// Populated after a def's body finishes compiling (see recordFunctionDependsOnInfo); absent for a
		// signature that's registered (addLocalFunction) but not yet fully compiled -- self-recursive,
		// forward, or mutually-recursive references, which correctly fall back to a conservative default.
		final Map<FunctionSignature, FunctionDependsOnInfo> functionDependsOnInfo = new HashMap<>();
		// The declared parameter names of each local function, recorded by addLocalFunction's caller before
		// the def's body compiles -- so a call inside that body, recursive or not, can still see them. A tail
		// call needs them because a `$name` parameter takes a value and a filter parameter takes a Function,
		// and it has to produce the right one without the callee's bindAndApply to sort it out.
		final Map<FunctionSignature, List<String>> functionParameterNames = new HashMap<>();
		// True only for a scope Compiler pushed for a `def` in the source it is lowering. The implicit root
		// scope and a jq-library body are function boundaries too, but neither becomes a
		// ResolvedFunctionDefinition, so neither gets the loop that runs tail calls -- and a call must not be
		// compiled as a tail call with nothing to run it.
		boolean isDefinitionBody;
		// The signature and parameter slots of the `def` this scope is the body of, recorded before the body
		// compiles. A tail call to the def it is already in writes its new arguments straight into those
		// slots, which is the whole of what such a call does besides jumping.
		@Nullable FunctionSignature definitionSignature;
		@Nullable List<Integer> definitionParameterSlots;
		// The slot this body's tail calls leave their jump in, assigned on the first one compiled. -1 while
		// the body holds none, which is also what tells ResolvedFunctionDefinition it needs no loop.
		int tailCallSlot = -1;
		int nextSlot;

		// Reserved slot (within this function's own frame) that will hold this function's own Closure at
		// runtime -- the "static link" slot. Set once, right after params, before the body is compiled, so
		// that nested defs compiled while this scope is on top can reference it as a fixed, known-in-advance
		// constant. -1 until reserveClosureSlot() is called; never called for scopes that aren't the subject
		// of a `def` (the implicit root scope, or a local scope).
		int closureSlot = -1;

		// Shared index space for names captured into this function's own Closure -- one flat numbering for
		// both captured variables and captured functions, mirroring how nextSlot is already shared between
		// locals of both kinds.
		int nextClosureSlot;

		final List<ClosureSpec.CapturedVariableRef> capturedVariables = new ArrayList<>();
		final Map<String, Integer> capturedVarSlots = new HashMap<>();
		final Map<String, BoundArgumentInfo> capturedVarBoundArguments = new HashMap<>();

		final List<ClosureSpec.CapturedFunctionRef> capturedFunctions = new ArrayList<>();
		final Map<FunctionSignature, Integer> capturedFnSlots = new HashMap<>();
		final Map<FunctionSignature, BoundArgumentInfo> capturedFnBoundArguments = new HashMap<>();

		ScopeFrame(boolean isFunctionBoundary, int initialSlot) {
			this(isFunctionBoundary, false, false, initialSlot);
		}

		ScopeFrame(boolean isFunctionBoundary, boolean isolatesLexicalScope, boolean foldsSlotCountOnPop, int initialSlot) {
			this.isFunctionBoundary = isFunctionBoundary;
			this.isolatesLexicalScope = isolatesLexicalScope;
			this.foldsSlotCountOnPop = foldsSlotCountOnPop;
			this.nextSlot = initialSlot;
		}
	}

	private static final class GlobalState {
		final Map<String, Integer> variableIndices = new HashMap<>();
		final Map<FunctionSignature, Integer> functionIndices = new HashMap<>();
		int nextIndex;
	}

	private final List<ScopeFrame> scopes;
	private final JqFunctionCompiler.State jqFunctionState;
	private final Set<JqFunctionCompiler.DefinitionKey> activeJqFunctions;
	private final Set<JqFunctionCompiler.DefinitionKey> genericJqFunctions;

	// Flat, lazily-allocated indices into StackMemory's global-slots array (see docs/stack-frame-closure-design.md)
	// for "declared" (EnvironmentBuilder.declareVariable/declareFunction) globals -- entirely independent of
	// scope nesting, mirroring how ScopeFrame#nextSlot/nextClosureSlot are already single shared counters.
	// Allocation is on-first-reference, so the key sets are exactly "the declared names this compiled query
	// actually uses." Unlike local/captured symbols, declared/defined globals never enter the scope-stack at
	// all -- Compiler.java resolves them directly against the Environment once the scope-stack walk finds
	// nothing, so there's no shadowing bookkeeping to do here.
	private final GlobalState globalState;

	private final boolean exportTopLevelFunctions;
	// Whether code compiled in this context draws on the per-invocation budgets a caller sets --
	// RuntimeOptions#setMaxUserDefinedFunctionCalls for a `def`, #setMaxOutputsPerExpression for an
	// expression's output. True only for the root context of a query the caller wrote (Compiler#compile);
	// false for a module source (Compiler#compileModule) and for every jq-library function body (the
	// create*JqFunctionContext factories below), so an internal `def` -- recurse's `def r`, say -- never
	// spends the caller's budget, and neither does anything it emits along the way.
	private final boolean meterRuntimeBudgets;

	// Whether CompileOptions asked for tail calls at all. Threaded into the jq-library child contexts too,
	// since the recursion in until/while/recurse lives in a nested def inside those bodies.
	private final boolean tailCallsEnabled;

	// Whether the node being lowered right now stands in tail position of the def body it belongs to. Unlike
	// everything else here this is a top-down fact, so Compiler saves and restores it around each child it
	// lowers rather than deriving it afterwards.
	private boolean tailPosition;

	// Next free index into Memory#outputCounts, handed out one per metered expression by
	// allocateOutputCounter(). Only a metered context ever allocates, so the numbering covers exactly one
	// top-level compile's query-text expressions and the array Memory sizes from it is never indexed by a
	// node compiled anywhere else.
	private int nextOutputCounter;

	// What the compilation has folded so far, and the budget it is folding within. Per compilation rather
	// than per node, because Compiler builds a fresh CompilationVisitor for every node it lowers while this
	// one object is threaded through all of them -- including the child contexts the
	// create*JqFunctionContext factories fork, which share it so the tally covers the whole compilation.
	private final FoldPlanner foldPlanner;

	private final Map<FunctionSignature, Integer> rootFunctionSlots;
	private final Map<String, JavaModule> importedModules;
	private final Map<String, Object> importedVariableDefaults;

	public CompileContext() {
		this(false, false);
	}

	public CompileContext(boolean exportTopLevelFunctions, boolean meterRuntimeBudgets) {
		this(exportTopLevelFunctions, meterRuntimeBudgets, OptimizationOptions.newBuilder().build());
	}

	public CompileContext(boolean exportTopLevelFunctions, boolean meterRuntimeBudgets, OptimizationOptions optimizationOptions) {
		this(exportTopLevelFunctions, meterRuntimeBudgets, optimizationOptions.getTailCallOptimization(), new JqFunctionCompiler.State(), Collections.emptySet(), Collections.emptySet(), new GlobalState(), new FoldPlanner(optimizationOptions.getConstantFoldingOptions()));
	}

	private CompileContext(boolean exportTopLevelFunctions, boolean meterRuntimeBudgets, boolean tailCallsEnabled, JqFunctionCompiler.State jqFunctionState, Set<JqFunctionCompiler.DefinitionKey> activeJqFunctions, Set<JqFunctionCompiler.DefinitionKey> genericJqFunctions, GlobalState globalState, FoldPlanner foldPlanner) {
		this(newRootScopes(), exportTopLevelFunctions, meterRuntimeBudgets, tailCallsEnabled, jqFunctionState, activeJqFunctions, genericJqFunctions, globalState, foldPlanner);
	}

	// Shares `scopes` with the caller rather than starting a fresh list -- used only to build an inlined
	// jq-function-compiler context (see createInlinedJqFunctionContext), which must still be a physically
	// separate CompileContext object (its own activeJqFunctions/genericJqFunctions fork), but needs to
	// keep allocating slots in whatever frame is already live on the caller's own scope stack rather than
	// starting a brand-new one.
	private CompileContext(List<ScopeFrame> scopes, boolean exportTopLevelFunctions, boolean meterRuntimeBudgets, boolean tailCallsEnabled, JqFunctionCompiler.State jqFunctionState, Set<JqFunctionCompiler.DefinitionKey> activeJqFunctions, Set<JqFunctionCompiler.DefinitionKey> genericJqFunctions, GlobalState globalState, FoldPlanner foldPlanner) {
		this.scopes = scopes;
		this.tailCallsEnabled = tailCallsEnabled;
		this.foldPlanner = foldPlanner;
		this.jqFunctionState = jqFunctionState;
		this.activeJqFunctions = activeJqFunctions;
		this.genericJqFunctions = genericJqFunctions;
		this.globalState = globalState;
		this.exportTopLevelFunctions = exportTopLevelFunctions;
		this.meterRuntimeBudgets = meterRuntimeBudgets;
		this.rootFunctionSlots = new HashMap<>();
		this.importedModules = new HashMap<>();
		this.importedVariableDefaults = new HashMap<>();
	}

	private static List<ScopeFrame> newRootScopes() {
		List<ScopeFrame> scopes = new ArrayList<>();
		scopes.add(new ScopeFrame(true, 0));
		return scopes;
	}

	JqFunctionCompiler.State jqFunctionState() {
		return jqFunctionState;
	}

	boolean isJqFunctionActive(JqFunctionCompiler.DefinitionKey key) {
		return activeJqFunctions.contains(key);
	}

	boolean isGenericJqFunctionActive(JqFunctionCompiler.DefinitionKey key) {
		return genericJqFunctions.contains(key);
	}

	CompileContext createJqFunctionContext(JqFunctionCompiler.DefinitionKey key, boolean shareGlobalState) {
		Set<JqFunctionCompiler.DefinitionKey> nestedActiveJqFunctions = new HashSet<>(activeJqFunctions);
		nestedActiveJqFunctions.add(key);
		return new CompileContext(false, false, tailCallsEnabled, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), genericJqFunctions, shareGlobalState ? globalState : new GlobalState(), foldPlanner);
	}

	// Like createJqFunctionContext, but shares this context's own `scopes` list instead of starting a
	// fresh one, so a subsequent pushInlinedFunctionScope() call on the returned context allocates slots
	// in whatever frame is already live here -- see pushInlinedFunctionScope's doc for the isolation
	// guarantee this still preserves despite the shared list.
	CompileContext createInlinedJqFunctionContext(JqFunctionCompiler.DefinitionKey key, boolean shareGlobalState) {
		Set<JqFunctionCompiler.DefinitionKey> nestedActiveJqFunctions = new HashSet<>(activeJqFunctions);
		nestedActiveJqFunctions.add(key);
		return new CompileContext(scopes, false, false, tailCallsEnabled, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), genericJqFunctions, shareGlobalState ? globalState : new GlobalState(), foldPlanner);
	}

	CompileContext createGenericJqFunctionContext(JqFunctionCompiler.DefinitionKey key, boolean shareGlobalState) {
		Set<JqFunctionCompiler.DefinitionKey> nestedActiveJqFunctions = new HashSet<>(activeJqFunctions);
		nestedActiveJqFunctions.add(key);
		Set<JqFunctionCompiler.DefinitionKey> nestedGenericJqFunctions = new HashSet<>(genericJqFunctions);
		nestedGenericJqFunctions.add(key);
		return new CompileContext(false, false, tailCallsEnabled, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), Collections.unmodifiableSet(nestedGenericJqFunctions), shareGlobalState ? globalState : new GlobalState(), foldPlanner);
	}

	public void addImportedModule(String alias, JavaModule module) {
		importedModules.put(alias, module);
	}

	public @Nullable JavaModule getImportedModule(String alias) {
		return importedModules.get(alias);
	}

	/**
	 * Records the resolved value of a {@code $}-style data import (e.g. {@code import "data" as $name})
	 * as that variable's compile-time default -- collected here (not on the Environment, which the
	 * compiler must not mutate) and merged into the environment's variable defaults once compilation of
	 * the root expression finishes.
	 */
	public void addImportedVariableDefault(String name, Object value) {
		importedVariableDefaults.put(name, value);
	}

	public Map<String, Object> importedVariableDefaults() {
		return importedVariableDefaults;
	}

	/**
	 * Whether top-level {@code def}s compiled in this context should be tracked (via
	 * {@link #recordRootFunctionSlot}) for later harvesting into a module's exported functions
	 * ({@link Compiler#compileModule}). False for ordinary query compilation.
	 */
	public boolean exportsTopLevelFunctions() {
		return exportTopLevelFunctions;
	}

	/**
	 * Whether code compiled in this context counts against the per-invocation budgets a caller sets --
	 * {@code RuntimeOptions.Builder#setMaxUserDefinedFunctionCalls(long)} for a {@code def},
	 * {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)} for an expression's output. True only
	 * while compiling the jq text the caller handed to {@code Environment.compile()}; a module source and
	 * every jq-library function body compile unmetered, so a budget means the same thing no matter which
	 * builtins a query happens to use.
	 */
	public boolean metersRuntimeBudgets() {
		return meterRuntimeBudgets;
	}

	/**
	 * Whether a call in tail position may be compiled as a tail call.
	 *
	 * @return {@code true} unless {@code CompileOptions} turned the optimization off
	 */
	public boolean tailCallsEnabled() {
		return tailCallsEnabled;
	}

	/**
	 * Whether the node being lowered stands in tail position -- nothing its enclosing {@code def} body still
	 * has in progress will emit anything once it returns.
	 *
	 * @return {@code true} in tail position
	 */
	public boolean isTailPosition() {
		return tailPosition;
	}

	/**
	 * Sets whether the node about to be lowered stands in tail position, returning what it was so the caller
	 * can put it back. Every lowering step either passes it on to the one child that inherits it, or clears
	 * it; see {@code Compiler}.
	 *
	 * @param tailPosition the new value
	 * @return the previous value, to restore
	 */
	public boolean setTailPosition(boolean tailPosition) {
		boolean previous = this.tailPosition;
		this.tailPosition = tailPosition;
		return previous;
	}

	/**
	 * Marks the scope just pushed as a {@code def} body being lowered -- one that will get a loop to run
	 * whatever tail calls it turns out to contain -- and records what a tail call back into it needs: which
	 * signature identifies it, and which slots its parameters live in.
	 *
	 * @param signature the {@code def}'s signature
	 * @param parameterSlots its parameters' frame slots, in declaration order
	 */
	public void markDefinitionScope(FunctionSignature signature, List<Integer> parameterSlots) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.isDefinitionBody = true;
		top.definitionSignature = signature;
		top.definitionParameterSlots = parameterSlots;
	}

	/**
	 * The signature of the {@code def} whose body is being lowered, or {@code null} outside one. A call whose
	 * own signature differs cannot be a call back into it.
	 *
	 * @return the signature, or {@code null}
	 */
	public @Nullable FunctionSignature enclosingDefinitionSignature() {
		ScopeFrame enclosing = enclosingFunctionScope();
		return enclosing != null ? enclosing.definitionSignature : null;
	}

	/**
	 * The parameter slots of the {@code def} whose body is being lowered, for a tail call back into it to
	 * write into.
	 *
	 * @return the slots in declaration order, or {@code null} outside a {@code def} body
	 */
	public @Nullable List<Integer> enclosingDefinitionParameterSlots() {
		ScopeFrame enclosing = enclosingFunctionScope();
		return enclosing != null ? enclosing.definitionParameterSlots : null;
	}

	/**
	 * Whether the innermost enclosing function scope is a {@code def} body (see {@link #markDefinitionScope(FunctionSignature, List)})
	 * -- and therefore whether there will be anything to run a tail call taken here.
	 *
	 * @return {@code true} if a tail call taken now would have a loop to run it
	 */
	public boolean isInsideDefinitionBody() {
		ScopeFrame enclosing = enclosingFunctionScope();
		return enclosing != null && enclosing.isDefinitionBody;
	}

	/**
	 * The frame slot this {@code def} body's tail calls leave their jump in, claiming one on the first call
	 * that asks.
	 * <p>
	 * Claimed lazily, unlike {@link #reserveClosureSlot()}, because nothing outside this body ever needs the
	 * number: the call sites that read it are compiled within the body, and {@link #getSlotCount()} is read
	 * after the body finishes, so a slot claimed part-way through is still counted. Claimed from the
	 * enclosing function scope, which is also the scope on top -- no construct that pushes a local scope
	 * ({@code as}, {@code reduce}, {@code foreach}) passes tail position into it.
	 *
	 * @return the slot
	 */
	public int getOrAssignTailCallSlot() {
		ScopeFrame enclosing = enclosingFunctionScope();
		if (enclosing == null)
			throw new IllegalStateException("a tail call outside any function scope");
		if (enclosing.tailCallSlot < 0)
			enclosing.tailCallSlot = enclosing.nextSlot++;
		return enclosing.tailCallSlot;
	}

	/**
	 * The tail-call slot of the scope on top -- the {@code def} body that just finished compiling -- or
	 * {@code -1} if no tail call was compiled in it.
	 *
	 * @return the slot, or {@code -1}
	 */
	public int currentScopeTailCallSlot() {
		return scopes.get(scopes.size() - 1).tailCallSlot;
	}

	private @Nullable ScopeFrame enclosingFunctionScope() {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			ScopeFrame frame = scopes.get(i);
			if (frame.isFunctionBoundary)
				return frame;
		}
		return null;
	}

	/**
	 * Records the declared parameter names of a local function, for a call site that needs to know which
	 * parameters take a value and which take a filter. Called right after {@link #addLocalFunction}, before
	 * the body compiles, so a recursive call inside that body sees them.
	 *
	 * @param signature the function just declared in the current scope
	 * @param parameterNames its declared parameter names, {@code $}-prefixed for value parameters
	 */
	public void recordFunctionParameterNames(FunctionSignature signature, List<String> parameterNames) {
		scopes.get(scopes.size() - 1).functionParameterNames.put(signature, parameterNames);
	}

	/**
	 * Reserves this compile's next {@code Memory#outputCounts} index, for one expression whose output is to
	 * be tallied against {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)}.
	 *
	 * @return the reserved index
	 */
	public int allocateOutputCounter() {
		return nextOutputCounter++;
	}

	/**
	 * Reserves the output counter index that {@code child}'s consumer will charge, so it can count the values
	 * it consumes in the sink it already builds.
	 * <p>
	 * Each expression has exactly one consumer, so each is asked about exactly once and the index is simply
	 * the next one -- nothing has to be remembered per child.
	 * <p>
	 * Returns {@link Memory#NO_OUTPUT_COUNTER}, which costs nothing to charge, in three cases: an absent
	 * child, so a consumer with an optional child needs no special case; an unmetered context (a module
	 * source, a jq-library body), so no counting is compiled in there at all; and a child that cannot emit
	 * more than one value per input. That last one is the same reasoning the engine already applies to
	 * arguments in reverse: such a child runs only as often as whatever feeds it emits, and walking that
	 * chain up ends either at the root, at a generator, or at an argument -- all of which are counted -- so
	 * its own tally can never be the first to exceed the budget. Skipping it keeps {@code .foo}-shaped work
	 * free of counting entirely.
	 *
	 * @param child the expression whose values the caller consumes, or {@code null}
	 * @return the index to charge, or {@link Memory#NO_OUTPUT_COUNTER}
	 */
	public int outputCounterOf(@Nullable Expression<?, ?> child) {
		if (child == null || !meterRuntimeBudgets || child.getCardinality() != Cardinality.UNKNOWN)
			return Memory.NO_OUTPUT_COUNTER;
		return nextOutputCounter++;
	}

	/**
	 * {@link #outputCounterOf(Expression)} for a list of children, in order.
	 *
	 * @param children the expressions whose values the caller consumes
	 * @return one index per child
	 */
	public int[] outputCountersOf(List<? extends @Nullable Expression<?, ?>> children) {
		int[] indices = new int[children.size()];
		for (int i = 0; i < indices.length; ++i)
			indices[i] = outputCounterOf(children.get(i));
		return indices;
	}

	/**
	 * Records a node whose presence makes every subtree containing it unfoldable.
	 * <p>
	 * A barrier is a node that is not a function of its own subtree, so evaluating that subtree ahead of time
	 * does not reproduce what it does. There are two:
	 * <ul>
	 * <li>a {@code def} -- it registers its name in the <em>enclosing</em> lexical scope and writes its
	 * {@code Function} into the enclosing frame, so an expression compiled <em>after</em> the subtree can call
	 * it ({@code def f: 1; f}, and {@code def f: 1; . as $x | f} where the call is the {@code as} body).
	 * Folding would evaluate the install and discard it, leaving the later call an empty slot;</li>
	 * <li>a {@code try} or {@code ?} before jq 1.7 -- in that mode it catches errors raised
	 * <em>downstream</em> of it as well as inside it ({@code TryCatch.applyLegacy}), so what it does depends
	 * on who consumes its values. A fold consumes them with the compiler's own collector, which is not that
	 * consumer.</li>
	 * </ul>
	 */
	public void markFoldBarrier() {
		if (meterRuntimeBudgets)
			foldPlanner.markBarrier();
	}

	/**
	 * The compilation-wide constant-folding pass.
	 *
	 * @return this compilation's planner, never {@code null}
	 */
	public FoldPlanner foldPlanner() {
		return foldPlanner;
	}

	/**
	 * Returns how many output counters this compile reserved, which is the size of the
	 * {@code Memory#outputCounts} array an invocation of the resulting query needs.
	 *
	 * @return the number of reserved output counters
	 */
	public int getOutputCounterCount() {
		return nextOutputCounter;
	}

	/**
	 * Whether the current compile position is at the root scope -- i.e. not nested inside any {@code def},
	 * {@code as}, {@code reduce}, or {@code foreach} binding. Used to restrict
	 * {@link #exportsTopLevelFunctions()} tracking to a module's genuinely top-level {@code def}s, matching
	 * real jq module semantics (nested defs are private, not importable).
	 */
	public boolean isRootScope() {
		return scopes.size() == 1;
	}

	public void recordRootFunctionSlot(FunctionSignature key, int slot) {
		rootFunctionSlots.put(key, slot);
	}

	public Map<FunctionSignature, Integer> rootFunctionSlots() {
		return rootFunctionSlots;
	}


	public void pushLocalScope() {
		int currentSlot = scopes.isEmpty() ? 0 : scopes.get(scopes.size() - 1).nextSlot;
		scopes.add(new ScopeFrame(false, currentSlot));
	}

	public void pushFunctionScope() {
		scopes.add(new ScopeFrame(true, 0));
	}

	/**
	 * Pushes a scope for a JqFunctionCompiler call-site-specialized (inlined) library-function body.
	 * Function-boundary semantics apply (own params/nested defs behave like an ordinary def's, including
	 * closure-capture bookkeeping for anything nested inside it that reaches back to this scope's own
	 * params), but two things differ from {@link #pushFunctionScope()}:
	 * <ul>
	 * <li>name resolution is hard-isolated -- a lookup not satisfied inside this scope, or inside a real
	 * function scope nested within it, must never continue past it into whatever scopes are further out on
	 * the stack, so a library body can never resolve against a name that merely happens to be in scope at
	 * one particular call site (see the {@code isolatesLexicalScope} check in
	 * {@link #getVariableLocation} / {@link #getFunctionLocation});</li>
	 * <li>it shares its enclosing scope's physical frame -- slot numbering picks up where the enclosing
	 * scope's {@code nextSlot} left off, and folds back into it on {@link #popScope()}, instead of
	 * resetting to 0 and staying independent.</li>
	 * </ul>
	 * Must be paired with {@link #popScope()}.
	 *
	 * @return the absolute frame slot this scope's first local (its first param, by convention) will be
	 * assigned -- the caller needs this to translate this scope's own 0-based param index into an
	 * absolute slot number, since (unlike {@link #pushFunctionScope()}) slot numbering here does
	 * not start at 0.
	 */
	public int pushInlinedFunctionScope() {
		int currentSlot = scopes.isEmpty() ? 0 : scopes.get(scopes.size() - 1).nextSlot;
		scopes.add(new ScopeFrame(true, true, true, currentSlot));
		return currentSlot;
	}

	public void popScope() {
		if (scopes.size() > 1) {
			ScopeFrame popped = scopes.remove(scopes.size() - 1);
			if ((!popped.isFunctionBoundary || popped.foldsSlotCountOnPop) && !scopes.isEmpty()) {
				ScopeFrame top = scopes.get(scopes.size() - 1);
				if (popped.nextSlot > top.nextSlot) {
					top.nextSlot = popped.nextSlot;
				}
			}
		}
	}

	public int getSlotCount() {
		if (scopes.isEmpty())
			return 0;
		return scopes.get(scopes.size() - 1).nextSlot;
	}

	public ClosureSpec getClosureSpec() {
		if (scopes.isEmpty())
			return new ClosureSpec(Collections.emptyList(), Collections.emptyList());
		ScopeFrame top = scopes.get(scopes.size() - 1);
		return new ClosureSpec(new ArrayList<>(top.capturedVariables), new ArrayList<>(top.capturedFunctions));
	}

	/**
	 * Reserves the "static link" slot for the current (top) function scope: the fixed slot, within this
	 * function's own frame, that will hold this function's own Closure at runtime. Must be called on a
	 * function-boundary scope exactly once, immediately after its parameter slots are assigned and before
	 * its body is compiled, so that nested defs compiled while this scope is on top can reference the slot
	 * as a fixed, already-known constant (a capture crossing 2+ boundaries needs to know this number while
	 * the intermediate function's own frame layout is still being decided).
	 */
	public int reserveClosureSlot() {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		int slot = top.nextSlot++;
		top.closureSlot = slot;
		return slot;
	}

	/**
	 * The closure slot (see {@link #reserveClosureSlot()}) of the nearest enclosing function-boundary scope --
	 * i.e. where, in the frame that's live while code at the current compile position is executing, that
	 * function's own Closure will be found at runtime. Returns -1 if no enclosing scope ever reserved one
	 * (the implicit root scope), which is harmless: a top-level def's captures never need to read through it,
	 * since there is nothing beyond root to capture from.
	 */
	public int getCurrentFunctionClosureSlot() {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			ScopeFrame frame = scopes.get(i);
			if (frame.isFunctionBoundary)
				return frame.closureSlot;
		}
		return -1;
	}

	public void addLocalVariable(String name) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.variables.add(name);
		getOrAssignVariableSlotInTop(name);
	}

	public void addLocalVariable(String name, BoundArgumentInfo boundArgumentInfo) {
		addLocalVariable(name);
		scopes.get(scopes.size() - 1).variableBoundArguments.put(name, boundArgumentInfo);
	}

	public void addLocalFunction(FunctionSignature signature) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.functions.add(signature);
		getOrAssignFunctionSlotInTop(signature);
	}

	public void addLocalFunction(FunctionSignature signature, BoundArgumentInfo boundArgumentInfo) {
		addLocalFunction(signature);
		scopes.get(scopes.size() - 1).functionBoundArguments.put(signature, boundArgumentInfo);
	}

	/**
	 * Records precomputed {@code dependsOn*()} facts (see {@link FunctionDependsOnInfo}) for the local
	 * function {@code signature} just declared in the current (top) scope -- called once its
	 * {@code ResolvedFunctionDefinition} finishes compiling, after popping back out of its body's own
	 * function scope, so this lands in the same {@code ScopeFrame} {@link #addLocalFunction} registered it
	 * in.
	 */
	public void recordFunctionDependsOnInfo(FunctionSignature signature, FunctionDependsOnInfo info) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.functionDependsOnInfo.put(signature, info);
	}

	private int getOrAssignVariableSlotInTop(String name) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		Integer slot = top.variableSlots.get(name);
		if (slot != null)
			return slot;
		int assigned = top.nextSlot++;
		top.variableSlots.put(name, assigned);
		return assigned;
	}

	private int getOrAssignFunctionSlotInTop(FunctionSignature key) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		Integer slot = top.functionSlots.get(key);
		if (slot != null)
			return slot;
		int assigned = top.nextSlot++;
		top.functionSlots.put(key, assigned);
		return assigned;
	}

	/**
	 * Returns the {@link Memory} global-slot index for the declared
	 * variable {@code name}, assigning a fresh one on first reference.
	 */
	public int getOrAssignGlobalVariableIndex(String name) {
		return globalState.variableIndices.computeIfAbsent(name, ignored -> globalState.nextIndex++);
	}

	/**
	 * Returns the {@link Memory} global-slot index for the declared
	 * function {@code key}, assigning a fresh one on first reference.
	 */
	public int getOrAssignGlobalFunctionIndex(FunctionSignature key) {
		return globalState.functionIndices.computeIfAbsent(key, ignored -> globalState.nextIndex++);
	}

	public Map<String, Integer> globalVariableIndices() {
		return globalState.variableIndices;
	}

	public Map<FunctionSignature, Integer> globalFunctionIndices() {
		return globalState.functionIndices;
	}

	/**
	 * Total number of {@link Memory} global slots this compiled query
	 * needs -- the size to allocate its {@code StackMemory}'s globals array with.
	 */
	public int getGlobalCount() {
		return globalState.nextIndex;
	}

	public boolean isLocalVariable(String name) {
		return getVariableLocation(name) != null;
	}

	public int getVariableSlot(String name) {
		SymbolLocation loc = getVariableLocation(name);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public int getFunctionSlot(FunctionSignature signature) {
		SymbolLocation loc = getFunctionLocation(signature);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public @Nullable SymbolLocation getVariableLocation(String name) {
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		if (current.variables.contains(name)) {
			Integer slot = current.variableSlots.get(name);
			if (slot != null) {
				BoundArgumentInfo boundArgumentInfo = current.variableBoundArguments.get(name);
				return boundArgumentInfo != null ? SymbolLocation.localBoundArgument(slot, boundArgumentInfo) : SymbolLocation.local(slot);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			// The scope we're about to leave (i+1, which is `current` on the first iteration) may not be
			// walked past at all if it hard-isolates -- checked before touching `outer` (i), unlike the
			// crossedFunctionBoundary check just below, which still allows the walk to continue (via the
			// ordinary capture mechanism) once it's crossed an ordinary function boundary.
			if (scopes.get(i + 1).isolatesLexicalScope)
				break;

			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			if (outer.variables.contains(name)) {
				Integer localSlot = outer.variableSlots.get(name);
				if (localSlot != null) {
					BoundArgumentInfo boundArgumentInfo = outer.variableBoundArguments.get(name);
					if (!crossedFunctionBoundary) {
						return boundArgumentInfo != null ? SymbolLocation.localBoundArgument(localSlot, boundArgumentInfo) : SymbolLocation.local(localSlot);
					}
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
						if (closureSlot == null) {
							closureSlot = targetFrame.nextClosureSlot++;
							targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot, closureSlot));
							targetFrame.capturedVarSlots.put(name, closureSlot);
							if (boundArgumentInfo != null)
								targetFrame.capturedVarBoundArguments.put(name, boundArgumentInfo);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return boundArgumentInfo != null ? SymbolLocation.capturedBoundArgument(targetSlot, boundArgumentInfo) : SymbolLocation.captured(targetSlot);
				}
			}
			Integer existingClosureSlot = outer.capturedVarSlots.get(name);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				BoundArgumentInfo boundArgumentInfo = outer.capturedVarBoundArguments.get(name);
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
					if (closureSlot == null) {
						closureSlot = targetFrame.nextClosureSlot++;
						targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot, closureSlot));
						targetFrame.capturedVarSlots.put(name, closureSlot);
						if (boundArgumentInfo != null)
							targetFrame.capturedVarBoundArguments.put(name, boundArgumentInfo);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return boundArgumentInfo != null ? SymbolLocation.capturedBoundArgument(targetSlot, boundArgumentInfo) : SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}

	public @Nullable SymbolLocation getFunctionLocation(FunctionSignature key) {
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		FunctionSignature currentKey = resolveFunctionKey(current, key);
		if (currentKey != null) {
			Integer slot = current.functionSlots.get(currentKey);
			if (slot != null) {
				BoundArgumentInfo boundArgumentInfo = current.functionBoundArguments.get(currentKey);
				List<String> parameterNames = current.functionParameterNames.get(currentKey);
				if (boundArgumentInfo != null)
					return SymbolLocation.withParameterNames(SymbolLocation.localBoundArgument(slot, boundArgumentInfo), parameterNames);
				return SymbolLocation.withParameterNames(SymbolLocation.local(slot, current.functionDependsOnInfo.get(currentKey)), parameterNames);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			// See the matching comment in getVariableLocation: a hard-isolating scope we're about to leave
			// (i+1, `current` on the first iteration) stops the walk before `outer` (i) is examined at all.
			if (scopes.get(i + 1).isolatesLexicalScope)
				break;

			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			FunctionSignature outerKey = resolveFunctionKey(outer, key);
			if (outerKey != null) {
				Integer localSlot = outer.functionSlots.get(outerKey);
				if (localSlot != null) {
					BoundArgumentInfo boundArgumentInfo = outer.functionBoundArguments.get(outerKey);
					List<String> parameterNames = outer.functionParameterNames.get(outerKey);
					if (!crossedFunctionBoundary) {
						if (boundArgumentInfo != null)
							return SymbolLocation.withParameterNames(SymbolLocation.localBoundArgument(localSlot, boundArgumentInfo), parameterNames);
						return SymbolLocation.withParameterNames(SymbolLocation.local(localSlot, outer.functionDependsOnInfo.get(outerKey)), parameterNames);
					}
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedFnSlots.get(outerKey);
						if (closureSlot == null) {
							closureSlot = targetFrame.nextClosureSlot++;
							targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot, closureSlot));
							targetFrame.capturedFnSlots.put(outerKey, closureSlot);
							if (boundArgumentInfo != null)
								targetFrame.capturedFnBoundArguments.put(outerKey, boundArgumentInfo);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return SymbolLocation.withParameterNames(boundArgumentInfo != null
							? SymbolLocation.capturedBoundArgument(targetSlot, boundArgumentInfo)
							: SymbolLocation.captured(targetSlot, outer.functionDependsOnInfo.get(outerKey)), parameterNames);
				}
			}
			FunctionSignature existingKey = outer.capturedFnSlots.containsKey(key) ? key : key.asVariadic();
			Integer existingClosureSlot = outer.capturedFnSlots.get(existingKey);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				BoundArgumentInfo boundArgumentInfo = outer.capturedFnBoundArguments.get(existingKey);
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedFnSlots.get(existingKey);
					if (closureSlot == null) {
						closureSlot = targetFrame.nextClosureSlot++;
						targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot, closureSlot));
						targetFrame.capturedFnSlots.put(existingKey, closureSlot);
						if (boundArgumentInfo != null)
							targetFrame.capturedFnBoundArguments.put(existingKey, boundArgumentInfo);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return boundArgumentInfo != null ? SymbolLocation.capturedBoundArgument(targetSlot, boundArgumentInfo) : SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}

	private static @Nullable FunctionSignature resolveFunctionKey(ScopeFrame frame, FunctionSignature key) {
		if (frame.functions.contains(key))
			return key;
		FunctionSignature variadicKey = key.asVariadic();
		return frame.functions.contains(variadicKey) ? variadicKey : null;
	}
}
