package com.siyeh.ig.psiutils;

import java.util.List;

import com.intellij.psi.PsiImportModuleStatement;
import com.intellij.psi.PsiJavaFile;

/**
 * Stand-in for the IntelliJ inspection-gadgets class, which is absent from the shaded
 * intellij-code-formatter jar. ImportHelper consults it only to decide whether a name is already
 * available without an explicit import: through java.lang, the current package, an implicitly
 * declared class, or a module import. None of those apply here, because the formatter runs with no
 * classpath and the code style forbids on-demand imports, so answering "no" everywhere is the
 * conservative choice: at worst an already-redundant import survives, never a needed one removed.
 */
public final class ImportUtils {
	/**
	 * Must stay a class, not an interface: ImportHelper invokes it with invokevirtual.
	 */
	public static class ImplicitImportChecker {
		public boolean isImplicitlyImported(String fqName, boolean isStatic) {
			return false;
		}
	}

	private ImportUtils() {
	}

	public static ImplicitImportChecker createImplicitImportChecker(PsiJavaFile file) {
		return new ImplicitImportChecker();
	}

	public static boolean hasOnDemandImportConflictWithImports(PsiJavaFile file, List<?> imports, String fqName, boolean strict, boolean checkNestedClasses) {
		return false;
	}

	public static boolean isAlreadyImported(PsiJavaFile file, String fqName) {
		return false;
	}

	public static List<PsiImportModuleStatement> optimizeModuleImports(PsiJavaFile file) {
		return List.of();
	}
}
