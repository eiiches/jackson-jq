package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class TopLevelAstNode implements AstNode {
	private final List<ImportStatement> imports;
	private final AstNode expr;
	private final @Nullable ModuleDirective moduleDirective;

	public TopLevelAstNode(@Nullable ModuleDirective moduleDirective, List<ImportStatement> imports, AstNode expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	public @Nullable ModuleDirective moduleDirective() {
		return moduleDirective;
	}

	public List<ImportStatement> imports() {
		return imports;
	}

	public AstNode expr() {
		return expr;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (moduleDirective != null) {
			s.append(moduleDirective);
			s.append("; ");
		}
		for (ImportStatement imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}

	public static class ImportStatement {
		public final String path;
		public final boolean dollarImport;
		public final @Nullable String name;
		private final @Nullable AstNode metadataExpr;

		public ImportStatement(String path, boolean dollarImport, @Nullable String name, @Nullable AstNode metadataExpr) {
			this.path = path;
			this.dollarImport = dollarImport;
			this.name = name;
			this.metadataExpr = metadataExpr;
		}

		public @Nullable AstNode metadataExpr() {
			return metadataExpr;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			if (name == null) {
				s.append("include \"");
				s.append(path);
				s.append("\"");
			} else {
				s.append("import \"");
				s.append(path);
				s.append("\" as ");
				if (dollarImport)
					s.append('$');
				s.append(name);
			}
			if (metadataExpr != null) {
				s.append(' ');
				s.append(metadataExpr);
			}
			return s.toString();
		}
	}

	public static class ModuleDirective {
		private final AstNode metadataExpr;

		public ModuleDirective(AstNode metadataExpr) {
			this.metadataExpr = metadataExpr;
		}

		public AstNode metadataExpr() {
			return metadataExpr;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("module ");
			s.append(metadataExpr);
			return s.toString();
		}
	}
}
