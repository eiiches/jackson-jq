package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

/**
 * A statically resolved Java call awaiting post-type-check argument finalization and binding.
 */
public final class UnboundFunctionCall<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final BindContext<JsonNode> bindContext;
	private final String name;
	private final Function factory;
	private final List<AnalyzedExpression<JsonNode>> args;
	private final List<TypeScheme<FunctionType>> typeSchemes;
	private final ExpressionProperties properties;

	public UnboundFunctionCall(BindContext<JsonNode> bindContext, String name, Function factory,
							   List<AnalyzedExpression<JsonNode>> args, List<TypeScheme<FunctionType>> typeSchemes) {
		this.bindContext = bindContext;
		this.name = name;
		this.factory = factory;
		this.args = List.copyOf(args);
		this.typeSchemes = List.copyOf(typeSchemes);
		this.properties = factory.analyze(bindContext.getJqVersion(), propertiesOf(args));
	}

	public BindContext<JsonNode> bindContext() {
		return bindContext;
	}

	public Function factory() {
		return factory;
	}

	public String name() {
		return name;
	}

	public List<AnalyzedExpression<JsonNode>> args() {
		return args;
	}

	public List<TypeScheme<FunctionType>> typeSchemes() {
		return typeSchemes;
	}

	@Override
	public net.thisptr.jackson.jq.v2.spi.Cardinality getCardinality() {
		return properties.cardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return properties.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return properties.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.unionAll(args);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaqueIn(args);
	}

	@Override
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		List<AnalyzedExpression<JsonNode>> rewritten = ExpressionRewriter.rewriteAll(args, rewriter);
		return rewritten == args ? this : new UnboundFunctionCall<>(bindContext, name, factory, rewritten, typeSchemes);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		throw new IllegalStateException("Unbound static function call reached execution");
	}

	private static List<ExpressionProperties> propertiesOf(List<? extends AnalyzedExpression<?>> expressions) {
		return expressions.stream().map(AnalyzedExpression::propertiesOf).toList();
	}
}
