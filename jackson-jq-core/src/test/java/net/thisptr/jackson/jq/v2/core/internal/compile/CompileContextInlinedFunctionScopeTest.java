package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Direct, low-level coverage for pushInlinedFunctionScope()/popScope() -- the mechanism
// JqFunctionCompiler's frame-elided ("inlined") call-site specialization relies on to share a
// jq-library function's param/local slots with whatever real frame already encloses its call site,
// without losing lexical isolation from that caller's own local defs/variables.
public class CompileContextInlinedFunctionScopeTest {
	@Test
	public void slotNumberingSeedsFromAndFoldsIntoTheEnclosingScope() {
		CompileContext context = new CompileContext();
		context.addLocalVariable("a");
		assertThat(context.getSlotCount()).isEqualTo(1);

		int baseSlot = context.pushInlinedFunctionScope();
		assertThat(baseSlot).isEqualTo(1);

		context.addLocalVariable("b");
		assertThat(Objects.requireNonNull(context.getVariableLocation("b")).slot).isEqualTo(baseSlot);
		assertThat(context.getSlotCount()).isEqualTo(2);

		context.popScope();

		// the inlined scope's own high-water mark (2) folded back into the enclosing scope, exactly like
		// an ordinary (non-function-boundary) local scope's does -- unlike an ordinary pushFunctionScope(),
		// whose slot count never affects the caller.
		assertThat(context.getSlotCount()).isEqualTo(2);
		assertThat(Objects.requireNonNull(context.getVariableLocation("a")).slot).isEqualTo(0);
	}

	@Test
	public void nameBoundOnlyOutsideTheInlinedScopeDoesNotResolveFromWithinIt() {
		CompileContext context = new CompileContext();
		context.addLocalVariable("outer");

		context.pushInlinedFunctionScope();
		try {
			assertThat(context.isLocalVariable("outer")).isFalse();
			assertThat(context.getVariableLocation("outer")).isNull();
		} finally {
			context.popScope();
		}

		// the name is visible again once back outside the (popped) inlined scope.
		assertThat(context.isLocalVariable("outer")).isTrue();
	}

	@Test
	public void nameBoundInsideTheInlinedScopeResolvesLocallyThere() {
		CompileContext context = new CompileContext();
		context.pushInlinedFunctionScope();
		try {
			context.addLocalVariable("p");
			SymbolLocation loc = Objects.requireNonNull(context.getVariableLocation("p"));
			assertThat(loc.isLocal).isTrue();
		} finally {
			context.popScope();
		}
	}

	@Test
	public void nestedRealFunctionScopeInsideTheInlinedScopeCapturesItsParamViaHopOne() {
		CompileContext context = new CompileContext();
		context.pushInlinedFunctionScope();
		try {
			context.addLocalVariable("p");

			// simulates an ordinary internal `def` nested inside a jq-library function's own body
			// (e.g. recurse's `def r: ...;`) -- a genuine function boundary of its own.
			context.pushFunctionScope();
			try {
				SymbolLocation loc = Objects.requireNonNull(context.getVariableLocation("p"));
				assertThat(loc.isLocal).isFalse();
			} finally {
				context.popScope();
			}
		} finally {
			context.popScope();
		}
	}

	@Test
	public void nestedRealFunctionScopeInsideTheInlinedScopeStaysIsolatedFromTheCallersOwnScope() {
		CompileContext context = new CompileContext();
		context.addLocalVariable("outer");

		context.pushInlinedFunctionScope();
		try {
			context.pushFunctionScope();
			try {
				assertThat(context.getVariableLocation("outer")).isNull();
			} finally {
				context.popScope();
			}
		} finally {
			context.popScope();
		}
	}
}
