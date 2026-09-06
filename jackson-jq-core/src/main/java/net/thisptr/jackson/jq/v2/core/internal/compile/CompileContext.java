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

import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.Module;

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
	private final Map<FunctionSignature, Integer> rootFunctionSlots;
	private final Map<String, Module> importedModules;
	private final Map<String, Object> importedVariableDefaults;

	// Whether the `.` currently being compiled against is known, at compile time, to always be the
	// same fixed value -- e.g. the output of an earlier, already-constant pipe stage. Saved/restored
	// (like a scope push/pop) around the handful of constructs that rebind what `.` means for a child
	// expression (pipe stage transitions, try/catch's catch, string-interpolation's formatter,
	// reduce/foreach's update expression, |='s rhs). Read by Compiler when constructing the few node
	// types that read `.` themselves or fall back to the raw input (see Expression#dependsOnInput).
	private boolean inputFixed = false;

	public boolean isInputFixed() {
		return inputFixed;
	}

	public void setInputFixed(boolean inputFixed) {
		this.inputFixed = inputFixed;
	}

	public CompileContext() {
		this(false);
	}

	public CompileContext(boolean exportTopLevelFunctions) {
		this(exportTopLevelFunctions, new JqFunctionCompiler.State(), Collections.emptySet(), Collections.emptySet(), new GlobalState());
	}

	private CompileContext(boolean exportTopLevelFunctions, JqFunctionCompiler.State jqFunctionState, Set<JqFunctionCompiler.DefinitionKey> activeJqFunctions, Set<JqFunctionCompiler.DefinitionKey> genericJqFunctions, GlobalState globalState) {
		this(newRootScopes(), exportTopLevelFunctions, jqFunctionState, activeJqFunctions, genericJqFunctions, globalState);
	}

	// Shares `scopes` with the caller rather than starting a fresh list -- used only to build an inlined
	// jq-function-compiler context (see createInlinedJqFunctionContext), which must still be a physically
	// separate CompileContext object (its own activeJqFunctions/genericJqFunctions fork), but needs to
	// keep allocating slots in whatever frame is already live on the caller's own scope stack rather than
	// starting a brand-new one.
	private CompileContext(List<ScopeFrame> scopes, boolean exportTopLevelFunctions, JqFunctionCompiler.State jqFunctionState, Set<JqFunctionCompiler.DefinitionKey> activeJqFunctions, Set<JqFunctionCompiler.DefinitionKey> genericJqFunctions, GlobalState globalState) {
		this.scopes = scopes;
		this.jqFunctionState = jqFunctionState;
		this.activeJqFunctions = activeJqFunctions;
		this.genericJqFunctions = genericJqFunctions;
		this.globalState = globalState;
		this.exportTopLevelFunctions = exportTopLevelFunctions;
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
		return new CompileContext(false, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), genericJqFunctions, shareGlobalState ? globalState : new GlobalState());
	}

	// Like createJqFunctionContext, but shares this context's own `scopes` list instead of starting a
	// fresh one, so a subsequent pushInlinedFunctionScope() call on the returned context allocates slots
	// in whatever frame is already live here -- see pushInlinedFunctionScope's doc for the isolation
	// guarantee this still preserves despite the shared list.
	CompileContext createInlinedJqFunctionContext(JqFunctionCompiler.DefinitionKey key, boolean shareGlobalState) {
		Set<JqFunctionCompiler.DefinitionKey> nestedActiveJqFunctions = new HashSet<>(activeJqFunctions);
		nestedActiveJqFunctions.add(key);
		return new CompileContext(scopes, false, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), genericJqFunctions, shareGlobalState ? globalState : new GlobalState());
	}

	CompileContext createGenericJqFunctionContext(JqFunctionCompiler.DefinitionKey key, boolean shareGlobalState) {
		Set<JqFunctionCompiler.DefinitionKey> nestedActiveJqFunctions = new HashSet<>(activeJqFunctions);
		nestedActiveJqFunctions.add(key);
		Set<JqFunctionCompiler.DefinitionKey> nestedGenericJqFunctions = new HashSet<>(genericJqFunctions);
		nestedGenericJqFunctions.add(key);
		return new CompileContext(false, jqFunctionState, Collections.unmodifiableSet(nestedActiveJqFunctions), Collections.unmodifiableSet(nestedGenericJqFunctions), shareGlobalState ? globalState : new GlobalState());
	}

	public void addImportedModule(String alias, Module module) {
		importedModules.put(alias, module);
	}

	public @Nullable Module getImportedModule(String alias) {
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

	public void addLocalFunction(String name, int arity) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		FunctionSignature key = FunctionSignature.of(name, arity);
		top.functions.add(key);
		getOrAssignFunctionSlotInTop(key);
	}

	public void addLocalFunction(String name, int arity, BoundArgumentInfo boundArgumentInfo) {
		addLocalFunction(name, arity);
		scopes.get(scopes.size() - 1).functionBoundArguments.put(FunctionSignature.of(name, arity), boundArgumentInfo);
	}

	/**
	 * Records precomputed {@code dependsOn*()} facts (see {@link FunctionDependsOnInfo}) for the local
	 * function {@code name}/{@code arity} just declared in the current (top) scope -- called once its
	 * {@code ResolvedFunctionDefinition} finishes compiling, after popping back out of its body's own
	 * function scope, so this lands in the same {@code ScopeFrame} {@link #addLocalFunction} registered it
	 * in.
	 */
	public void recordFunctionDependsOnInfo(String name, int arity, FunctionDependsOnInfo info) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.functionDependsOnInfo.put(FunctionSignature.of(name, arity), info);
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

	public boolean isLocalFunction(String name, int arity) {
		return getFunctionLocation(name, arity) != null;
	}

	public int getVariableSlot(String name) {
		SymbolLocation loc = getVariableLocation(name);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public int getFunctionSlot(String name, int arity) {
		SymbolLocation loc = getFunctionLocation(name, arity);
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

	public @Nullable SymbolLocation getFunctionLocation(String name, int arity) {
		FunctionSignature key = FunctionSignature.of(name, arity);
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		FunctionSignature currentKey = resolveFunctionKey(current, key);
		if (currentKey != null) {
			Integer slot = current.functionSlots.get(currentKey);
			if (slot != null) {
				BoundArgumentInfo boundArgumentInfo = current.functionBoundArguments.get(currentKey);
				if (boundArgumentInfo != null)
					return SymbolLocation.localBoundArgument(slot, boundArgumentInfo);
				return SymbolLocation.local(slot, current.functionDependsOnInfo.get(currentKey));
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
					if (!crossedFunctionBoundary) {
						if (boundArgumentInfo != null)
							return SymbolLocation.localBoundArgument(localSlot, boundArgumentInfo);
						return SymbolLocation.local(localSlot, outer.functionDependsOnInfo.get(outerKey));
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
					return boundArgumentInfo != null
							? SymbolLocation.capturedBoundArgument(targetSlot, boundArgumentInfo)
							: SymbolLocation.captured(targetSlot, outer.functionDependsOnInfo.get(outerKey));
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
