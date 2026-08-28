import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatter.bootstrap.FormatterBootstrap;
import com.intellij.formatter.config.CodeStyleLoader;
import com.intellij.formatter.core.FormatReport;
import com.intellij.formatter.core.FormattingException;
import com.intellij.formatter.core.JavaFileTraverser;
import com.intellij.formatter.core.StandaloneFormatter;
import com.intellij.lang.ASTNode;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.ProjectCodeStyleSettingsManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.codeStyle.FormatCommentsProcessor;

public final class IdeaFormatLauncher {
	private IdeaFormatLauncher() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			throw new IllegalArgumentException("Expected a style file followed by formatter arguments");
		}

		boolean checkMode = false;
		Path projectDir = null;
		for (int i = 1; i < args.length; ++i) {
			if (args[i].equals("--check")) {
				checkMode = true;
			} else {
				projectDir = Path.of(args[i]);
			}
		}
		if (projectDir == null) {
			throw new IllegalArgumentException("Expected a project directory argument");
		}

		FormatterBootstrap.initialize();
		FormatterBootstrap.ensureLanguageRegistered("Formatter.java");
		CodeStyleLoader.loadFromFile(args[0]);

		Project project = FormatterBootstrap.getProject();
		ProjectCodeStyleSettingsManager manager = project.getService(ProjectCodeStyleSettingsManager.class);
		CodeStyleSettings settings = manager.getMainProjectCodeStyle();
		CodeStyle.setMainProjectSettings(project, settings);

		List<Path> files = JavaFileTraverser.findJavaFiles(projectDir);
		List<Path> changed = new ArrayList<>();
		Map<Path, String> failures = new LinkedHashMap<>();

		for (Path path : files) {
			String fileName = path.getFileName().toString();
			try {
				String original = Files.readString(path);
				String withCommentsFormatted = formatJavadocCommentsUntilStable(project, original, fileName);
				String finalText = StandaloneFormatter.formatCode(withCommentsFormatted, fileName);
				if (!finalText.equals(original)) {
					changed.add(path);
					if (!checkMode) {
						Files.writeString(path, finalText);
					}
				}
			} catch (FormattingException e) {
				failures.put(path, e.getMessage());
			} catch (IOException e) {
				failures.put(path, "I/O error: " + e.getMessage());
			}
		}

		FormatReport report = new FormatReport(files.size(), changed, failures);
		System.exit(printReportAndComputeExitCode(report, checkMode));
	}

	// A single FormatCommentsProcessor pass sometimes only reflows the first Javadoc comment in a
	// file when several need the same short-to-multiline expansion (an upstream JDParser quirk,
	// not settings-dependent), leaving the rest for a later run. Re-running the pass against its
	// own output until it stabilizes gets every comment reformatted within one invocation instead
	// of requiring the developer to run the script repeatedly to converge.
	private static String formatJavadocCommentsUntilStable(Project project, String text, String fileName) {
		String current = text;
		for (int i = 0; i < 5; ++i) {
			String next = formatJavadocComments(project, current, fileName);
			if (next.equals(current)) {
				break;
			}
			current = next;
		}
		return current;
	}

	// intellij-code-formatter never registers Java's FormatCommentsProcessor as a
	// PreFormatProcessor, and running it through the normal reformat() pipeline desyncs the PSI
	// tree from the Document, crashing the block formatter. Running it as its own in-memory pass
	// first, then handing the result to StandaloneFormatter.formatCode() for layout, avoids both
	// problems. Any unexpected Javadoc shape falls back to the original text rather than risking
	// corruption or aborting the whole run.
	private static String formatJavadocComments(Project project, String text, String fileName) {
		try {
			PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaLanguage.INSTANCE, text);
			ASTNode node = ((PsiFileImpl) psiFile).calcTreeElement();
			PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
			Document document = documentManager.getDocument(psiFile);

			CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
				new FormatCommentsProcessor().process(node, TextRange.create(0, node.getTextLength()));
				if (document != null) {
					documentManager.doPostponedOperationsAndUnblockDocument(document);
				}
			}), "Format Javadoc", null);

			return psiFile.getText();
		} catch (Throwable t) {
			System.err.println("Warning: Javadoc formatting failed for " + fileName + ": " + t);
			return text;
		}
	}

	private static int printReportAndComputeExitCode(FormatReport report, boolean checkMode) {
		if (!checkMode) {
			report.changed().forEach(path -> System.out.println("Formatted: " + path));
			System.out.println("Formatted " + report.changed().size() + " of " + report.totalFiles() + " files");
		} else {
			report.changed().forEach(path -> System.out.println("Not formatted: " + path));
			System.out.println(report.changed().size() + " of " + report.totalFiles() + " files need formatting");
		}
		report.failures().forEach((path, message) -> System.err.println("Failed: " + path + " - " + message));

		if (!report.failures().isEmpty()) {
			return 2;
		}
		if (checkMode && !report.changed().isEmpty()) {
			return 1;
		}
		return 0;
	}
}
