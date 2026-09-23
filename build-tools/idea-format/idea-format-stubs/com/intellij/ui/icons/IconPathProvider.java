package com.intellij.ui.icons;

/**
 * Stand-in for the real IntelliJ platform interface, which is absent from the shaded
 * intellij-code-formatter jar. It is reached only through an instanceof test on an icon
 * implementation while PSI elements are being described, so an empty interface suffices.
 */
public interface IconPathProvider {
}
