package com.intellij.psi.jsp;

import com.intellij.psi.PsiFile;

/**
 * Stand-in for the real IntelliJ platform interface, which is absent from the shaded
 * intellij-code-formatter jar. ImportHelper reaches it through JspPsiUtil only to test whether the
 * file being optimized is a JSP; an empty interface nothing implements makes that test answer no.
 */
public interface BaseJspFile extends PsiFile {
}
