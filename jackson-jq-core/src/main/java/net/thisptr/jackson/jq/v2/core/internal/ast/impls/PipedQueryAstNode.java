package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.PatternMatcherAstNode;

public class PipedQueryAstNode implements AstNode {
	private List<PipeComponent> components;

	public PipedQueryAstNode(List<PipeComponent> components) {
		this.components = components;
	}

	public List<PipeComponent> components() {
		return components;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		@Var String sep = "";
		for (PipeComponent component : components) {
			builder.append(sep);
			builder.append(component.toString());
			sep = " | ";
		}
		return builder.toString();
	}

	public interface PipeComponent extends AstNode {

		boolean canTerminatePipe();
	}

	public static class AssignPipeComponent implements PipeComponent {
		public final AstNode expr;
		public final PatternMatcherAstNode matcher;

		public AssignPipeComponent(AstNode expr, PatternMatcherAstNode matcher) {
			this.expr = expr;
			this.matcher = matcher;
		}

		@Override
		public boolean canTerminatePipe() {
			return false;
		}

		@Override
		public String toString() {
			return expr + " as " + matcher;
		}
	}

	public static class LabelPipeComponent implements PipeComponent {
		public final String name;

		public LabelPipeComponent(String name) {
			this.name = name;
		}

		@Override
		public boolean canTerminatePipe() {
			return false;
		}

		@Override
		public String toString() {
			return "label $" + name;
		}
	}

	public static class TransformPipeComponent implements PipeComponent {
		public final AstNode expr;

		public TransformPipeComponent(AstNode expr) {
			this.expr = expr;
		}

		@Override
		public boolean canTerminatePipe() {
			return true;
		}

		@Override
		public String toString() {
			return expr.toString();
		}
	}
}
