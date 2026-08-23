import java.lang.reflect.Method;
import java.util.Arrays;

public final class IdeaFormatLauncher {
	private IdeaFormatLauncher() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			throw new IllegalArgumentException("Expected a style file followed by formatter arguments");
		}

		Class<?> bootstrapClass = Class.forName("com.intellij.formatter.bootstrap.FormatterBootstrap");
		bootstrapClass.getMethod("initialize").invoke(null);
		bootstrapClass.getMethod("ensureLanguageRegistered", String.class).invoke(null, "Formatter.java");

		Class<?> loaderClass = Class.forName("com.intellij.formatter.config.CodeStyleLoader");
		loaderClass.getMethod("loadFromFile", String.class).invoke(null, args[0]);

		Class<?> projectClass = Class.forName("com.intellij.openapi.project.Project");
		Object project = bootstrapClass.getMethod("getProject").invoke(null);
		Class<?> managerClass = Class.forName("com.intellij.psi.codeStyle.ProjectCodeStyleSettingsManager");
		Object manager = projectClass.getMethod("getService", Class.class).invoke(project, managerClass);
		Object settings = managerClass.getMethod("getMainProjectCodeStyle").invoke(manager);

		Class<?> settingsClass = Class.forName("com.intellij.psi.codeStyle.CodeStyleSettings");
		Class<?> codeStyleClass = Class.forName("com.intellij.application.options.CodeStyle");
		Method activateSettings = codeStyleClass.getMethod("setMainProjectSettings", projectClass, settingsClass);
		activateSettings.invoke(null, project, settings);

		Class<?> applicationClass = Class.forName("com.intellij.formatter.JetbrainsFormatterApplication");
		String[] formatterArgs = Arrays.copyOfRange(args, 1, args.length);
		applicationClass.getMethod("main", String[].class).invoke(null, (Object) formatterArgs);
	}
}
