import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockProject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.openapi.projectRoots.JavaVersionServiceImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.PsiSubstitutorFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.psi.codeStyle.ProjectCodeStyleSettingsManager;
import com.intellij.psi.controlFlow.ControlFlowFactory;
import com.intellij.psi.impl.PsiNameHelperImpl;
import com.intellij.psi.impl.PsiSubstitutorFactoryImpl;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.codeStyle.FormatCommentsProcessor;
import com.intellij.psi.impl.source.codeStyle.ImportHelper;
import com.intellij.psi.impl.source.resolve.JavaResolveCache;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.javadoc.JavadocManager;
import com.intellij.psi.javadoc.JavadocTagInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

public final class IdeaFormatLauncher {
	private static final Option OPT_CHECK = Option.builder()
			.longOpt("check")
			.desc("verify formatting without changing files")
			.get();
	private static final Option OPT_STYLE = Option.builder("s")
			.longOpt("style")
			.desc("path to code style XML (defaults to build-tools/idea-format/idea-code-style.xml)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.get();

	private IdeaFormatLauncher() {
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(OPT_CHECK);
		options.addOption(OPT_STYLE);
		options.addOption(OPT_HELP);

		CommandLine command;
		List<String> rest;
		try {
			CommandLineParser parser = new DefaultParser();
			command = parser.parse(options, args);
			rest = command.getArgList();
		} catch (ParseException e) {
			System.err.println("invalid arguments: " + Arrays.toString(args));
			System.exit(1);
			throw e;
		}

		if (command.hasOption(OPT_HELP.getOpt()) || command.hasOption(OPT_HELP.getLongOpt())) {
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("bazelisk run //:idea-format [-- [OPTIONS...] [TARGET...]]", null, options, null, false);
			System.exit(0);
		}

		boolean checkMode = command.hasOption(OPT_CHECK.getLongOpt());
		String stylePath = command.getOptionValue(OPT_STYLE.getLongOpt());

		String envWorkspace = System.getenv("BUILD_WORKSPACE_DIRECTORY");
		Path workspaceDir = (envWorkspace != null && !envWorkspace.isBlank())
				? Path.of(envWorkspace)
				: Path.of("").toAbsolutePath();

		String envWorking = System.getenv("BUILD_WORKING_DIRECTORY");
		Path workingDir = (envWorking != null && !envWorking.isBlank())
				? Path.of(envWorking)
				: workspaceDir;

		if (stylePath == null && !rest.isEmpty() && rest.get(0).endsWith(".xml")) {
			stylePath = rest.get(0);
			rest = rest.subList(1, rest.size());
		}

		Path styleFile;
		if (stylePath != null) {
			Path p = Path.of(stylePath);
			styleFile = p.isAbsolute() ? p : workingDir.resolve(p);
		} else {
			styleFile = workspaceDir.resolve("build-tools/idea-format/idea-code-style.xml");
		}

		if (!Files.exists(styleFile)) {
			throw new IllegalArgumentException("Style file does not exist: " + styleFile);
		}

		List<Path> targets = new ArrayList<>();
		if (rest.isEmpty()) {
			targets.add(workspaceDir);
		} else {
			for (String r : rest) {
				Path p = Path.of(r);
				targets.add(p.isAbsolute() ? p : workingDir.resolve(p));
			}
		}

		FormatterBootstrap.initialize();
		FormatterBootstrap.ensureLanguageRegistered("Formatter.java");
		CodeStyleLoader.loadFromFile(styleFile.toString());

		Project project = FormatterBootstrap.getProject();
		ProjectCodeStyleSettingsManager manager = project.getService(ProjectCodeStyleSettingsManager.class);
		CodeStyleSettings settings = manager.getMainProjectCodeStyle();
		CodeStyle.setMainProjectSettings(project, settings);
		registerImportOptimizationServices(project);

		List<Path> files = new ArrayList<>();
		for (Path target : targets) {
			files.addAll(JavaFileTraverser.findJavaFiles(target));
		}
		List<Path> changed = new ArrayList<>();
		Map<Path, String> failures = new LinkedHashMap<>();
		int importsNotOptimized = 0;

		for (Path path : files) {
			String fileName = path.getFileName().toString();
			try {
				String original = Files.readString(path);
				String withImportsOptimized = original;
				if (isImportOptimizable(fileName)) {
					withImportsOptimized = optimizeImports(project, settings, original, fileName);
					if (withImportsOptimized == null) {
						withImportsOptimized = original;
						++importsNotOptimized;
					}
				}
				String withCommentsFormatted = formatJavadocCommentsUntilStable(project, withImportsOptimized, fileName);
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
		int exitCode = printReportAndComputeExitCode(report, checkMode);
		if (importsNotOptimized > 0) {
			System.out.println("Imports left untouched in " + importsNotOptimized + " files the standalone resolver could not analyze");
		}
		System.exit(exitCode);
	}

	// module-info.java and package-info.java attach their annotations to the module or package
	// declaration rather than to a class, and ImportHelper does not walk those declarations. Every
	// import they need therefore looks unused to it - @NullMarked and the types named in a
	// "provides ... with ..." clause would all be deleted - so they are excluded from the pass.
	private static boolean isImportOptimizable(String fileName) {
		return !fileName.equals("module-info.java") && !fileName.equals("package-info.java");
	}

	// ImportHelper is the engine behind IntelliJ's Optimize Imports: it drops imports nothing
	// references and rewrites the rest into the order that IMPORT_LAYOUT_TABLE describes. Nothing
	// resolves against a real classpath here, which is safe rather than destructive - an import
	// whose short name appears in an unresolved reference is always kept - but the resolver the
	// shaded jar ships is incomplete enough that some files make it throw. Returning null for those
	// lets the caller leave their imports exactly as they were instead of guessing.
	private static String optimizeImports(Project project, CodeStyleSettings settings, String text, String fileName) throws Exception {
		String[] result = { null };
		// prepareOptimizeImportsResult() reformats the import list it builds, and that reformat
		// asserts it is on the EDT. Splicing the result back in as text, rather than replacing the
		// PSI import list, avoids needing the treeCopyHandler extension point that is not registered.
		EventQueue.invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(() -> {
			try {
				PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaLanguage.INSTANCE, text);
				if (!(psiFile instanceof PsiJavaFile javaFile)) {
					result[0] = text;
					return;
				}
				PsiImportList oldList = javaFile.getImportList();
				if (oldList == null) {
					result[0] = text;
					return;
				}
				PsiImportList newList = new ImportHelper(settings.getCustomSettings(JavaCodeStyleSettings.class))
						.prepareOptimizeImportsResult(javaFile, anyImport -> true);
				if (newList == null) {
					result[0] = text;
					return;
				}
				TextRange range = oldList.getTextRange();
				result[0] = text.substring(0, range.getStartOffset()) + newList.getText() + text.substring(range.getEndOffset());
			} catch (Throwable t) {
				result[0] = null;
			}
		}));
		return result[0];
	}

	// FormatterBootstrap wires up only what layout formatting needs. Optimizing imports reaches
	// further into the Java PSI, so the services that path expects have to be supplied here. Where
	// the real implementation cannot be constructed without extension points that are never
	// registered, the stand-in answers "nothing to contribute", which costs ImportHelper no
	// information it would have acted on.
	private static void registerImportOptimizationServices(Project project) {
		MockApplication application = (MockApplication) ApplicationManager.getApplication();
		application.registerService(JavaVersionService.class, new JavaVersionServiceImpl());
		application.registerService(PsiSubstitutorFactory.class, new PsiSubstitutorFactoryImpl());
		application.registerService(ReferenceProvidersRegistry.class, new ReferenceProvidersRegistry() {
			@Override
			public PsiReferenceRegistrar getRegistrar(Language language) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void unloadProvidersFor(Language language) {
			}

			@Override
			protected PsiReference[] doGetReferencesFromProviders(PsiElement element, PsiReferenceService.Hints hints) {
				return PsiReference.EMPTY_ARRAY;
			}
		});

		MockProject mockProject = (MockProject) project;
		// PsiNameHelperImpl reads the language level from LanguageLevelProjectExtension, which this
		// project does not have. The level only gates which identifiers count as keywords, so
		// pinning it to the newest level the launcher itself compiles against is enough.
		mockProject.registerService(PsiNameHelper.class, new PsiNameHelperImpl(project) {
			@Override
			protected LanguageLevel getLanguageLevel() {
				return LanguageLevel.JDK_21;
			}
		});
		mockProject.registerService(ControlFlowFactory.class, new ControlFlowFactory(project));
		mockProject.registerService(JavaResolveCache.class, new JavaResolveCache(project));
		// JavadocManagerImpl cannot be constructed: it reads the com.intellij.javadocTagInfo
		// extension point at construction time and that point is never registered. Reporting no
		// known tags leaves {@link} and @see references resolving through the normal PSI path,
		// which is what keeps javadoc-only imports alive.
		mockProject.registerService(JavadocManager.class, new JavadocManager() {
			@Override
			public JavadocTagInfo[] getTagInfos(PsiElement context) {
				return new JavadocTagInfo[0];
			}

			@Override
			public JavadocTagInfo getTagInfo(String name) {
				return null;
			}
		});
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
