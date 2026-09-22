package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.errorprone.annotations.Var;
import dev.tamboui.backend.jline3.JLineBackend;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.exec.ExecPty;
import org.jline.terminal.impl.exec.ExecTerminalProvider;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.OptimizationOptions;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.fastjson2.Fastjson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Main {
	private enum VimSetting {
		AUTO,
		ENABLED,
		DISABLED
	}

	/**
	 * jq indents with two spaces.
	 */
	private static final String PRETTY_INDENT = "  ";
	/**
	 * jq reads the standard input for an input file named as a single dash.
	 */
	private static final String STDIN_FILE_NAME = "-";
	private static final Option OPT_COMPACT = Option.builder("c")
			.longOpt("compact")
			.desc("compact instead of pretty-printed output")
			.get();
	private static final Option OPT_COLOR_OUTPUT = Option.builder("C")
			.longOpt("color-output")
			.desc("colorize JSON output")
			.get();
	private static final Option OPT_MONOCHROME_OUTPUT = Option.builder("M")
			.longOpt("monochrome-output")
			.desc("disable colored output")
			.get();
	private static final Option OPT_RAW_OUTPUT = Option.builder("r")
			.longOpt("raw-output")
			.desc("output raw strings, not JSON texts")
			.get();
	private static final Option OPT_NULL_INPUT = Option.builder("n")
			.longOpt("null-input")
			.desc("use `null` as the single input value")
			.get();
	private static final Option OPT_RAW_INPUT = Option.builder("R")
			.longOpt("raw-input")
			.desc("read each line as string instead of JSON")
			.get();
	private static final Option OPT_SLURP = Option.builder("s")
			.longOpt("slurp")
			.desc("read all inputs into an array and use it as the single input value")
			.get();
	private static final Option OPT_FROM_FILE = Option.builder("f")
			.longOpt("from-file")
			.desc("load the filter from a file")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_VERSION = Option.builder()
			.longOpt("jq")
			.desc("specify jq version")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_JSON_PROVIDER = Option.builder()
			.longOpt("json-provider")
			.desc("JSON provider: jackson2, jackson3, fastjson2, gson, or jakarta (default: jackson3)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_NO_WARNINGS = Option.builder()
			.longOpt("no-warnings")
			.desc("suppress compile warnings")
			.get();
	private static final Option OPT_DISABLE_TCO = Option.builder()
			.longOpt("disable-tco")
			.desc("disable tail-call optimization")
			.get();
	private static final Option OPT_MAX_STRING_LENGTH = Option.builder()
			.longOpt("max-string-length")
			.desc("maximum length of strings produced during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_MAX_BINARY_LENGTH = Option.builder()
			.longOpt("max-binary-length")
			.desc("maximum number of bytes in binary values produced during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_MAX_ARRAY_LENGTH = Option.builder()
			.longOpt("max-array-length")
			.desc("maximum number of elements in arrays produced during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_MAX_OBJECT_MEMBER_COUNT = Option.builder()
			.longOpt("max-object-member-count")
			.desc("maximum number of members in objects produced during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_MAX_USER_DEFINED_FUNCTION_CALLS = Option.builder()
			.longOpt("max-user-defined-function-calls")
			.desc("maximum number of calls to functions defined in the query during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_MAX_OUTPUTS_PER_EXPRESSION = Option.builder()
			.longOpt("max-outputs-per-expression")
			.desc("maximum number of values a single expression in the query may produce during evaluation (default: unlimited)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_INTERACTIVE = Option.builder("i")
			.longOpt("interactive")
			.desc("interactive playground TUI")
			.get();
	private static final Option OPT_VIM = Option.builder()
			.longOpt("vim")
			.hasArg()
			.argName("auto|true|false")
			.desc("Vim keybindings: auto, true, or false (bare --vim means true and implies --interactive)")
			.get();
	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.get();

	static CommandLineParser createCommandLineParser() {
		CommandLineParser delegate = DefaultParser.builder()
				.setAllowPartialMatching(false)
				.get();
		return new CommandLineParser() {
			@Override
			public CommandLine parse(Options options, String[] arguments) throws ParseException {
				return delegate.parse(options, normalizeVimArguments(arguments));
			}

			@Override
			public CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption) throws ParseException {
				return delegate.parse(options, normalizeVimArguments(arguments), stopAtNonOption);
			}
		};
	}

	private static String[] normalizeVimArguments(String[] arguments) {
		String[] normalized = arguments.clone();
		for (int i = 0; i < normalized.length; i++) {
			if (normalized[i].equals("--")) {
				break;
			}
			if (normalized[i].equals("--vim")) {
				normalized[i] = "--vim=true";
			}
		}
		return normalized;
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(OPT_COMPACT);
		options.addOption(OPT_COLOR_OUTPUT);
		options.addOption(OPT_MONOCHROME_OUTPUT);
		options.addOption(OPT_RAW_OUTPUT);
		options.addOption(OPT_NULL_INPUT);
		options.addOption(OPT_RAW_INPUT);
		options.addOption(OPT_SLURP);
		options.addOption(OPT_FROM_FILE);
		options.addOption(OPT_VERSION);
		options.addOption(OPT_JSON_PROVIDER);
		options.addOption(OPT_NO_WARNINGS);
		options.addOption(OPT_DISABLE_TCO);
		options.addOption(OPT_MAX_STRING_LENGTH);
		options.addOption(OPT_MAX_BINARY_LENGTH);
		options.addOption(OPT_MAX_ARRAY_LENGTH);
		options.addOption(OPT_MAX_OBJECT_MEMBER_COUNT);
		options.addOption(OPT_MAX_USER_DEFINED_FUNCTION_CALLS);
		options.addOption(OPT_MAX_OUTPUTS_PER_EXPRESSION);
		options.addOption(OPT_INTERACTIVE);
		options.addOption(OPT_VIM);
		options.addOption(OPT_HELP);
		CommandLine command;
		List<String> rest;
		try {
			CommandLineParser parser = createCommandLineParser();
			command = parser.parse(options, args);
			rest = command.getArgList();
		} catch (ParseException e) {
			System.err.println("invalid arguments: " + Arrays.toString(args));
			System.exit(1);
			throw e;
		}
		@Var Version version = Versions.JQ_1_6;
		if (command.hasOption(OPT_VERSION.getLongOpt())) {
			version = Version.valueOf(command.getOptionValue(OPT_VERSION.getLongOpt()));
			if (!Versions.versions().contains(version)) {
				System.err.println("unsupported --jq version: " + version);
				System.exit(1);
			}
		}
		boolean interactive;
		try {
			interactive = isInteractive(command);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			throw e;
		}
		String queryFile = command.getOptionValue(OPT_FROM_FILE.getOpt());
		if ((queryFile == null && rest.isEmpty() && !interactive) || command.hasOption(OPT_HELP.getOpt())) {
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("jackson-jq [OPTIONS...] QUERY [FILE...]", null, options, null, false);
			System.exit(0);
		}
		String query;
		List<String> inputFiles;
		if (queryFile != null) {
			// jq reads the query from the file, so that every positional argument is an input file.
			try {
				query = Files.readString(Paths.get(queryFile));
			} catch (IOException e) {
				System.err.println("jq: error: Could not open " + queryFile + ": " + reason(e));
				System.exit(1);
				throw e;
			}
			inputFiles = rest;
		} else if (interactive) {
			if (!rest.isEmpty()) {
				query = rest.get(0);
				inputFiles = rest.subList(1, rest.size());
			} else {
				query = ".";
				inputFiles = rest;
			}
		} else {
			query = rest.get(0);
			inputFiles = rest.subList(1, rest.size());
		}
		String providerName = command.hasOption(OPT_JSON_PROVIDER.getLongOpt())
				? command.getOptionValue(OPT_JSON_PROVIDER.getLongOpt())
				: "jackson3";
		JsonProvider<?> jsonProvider;
		try {
			jsonProvider = resolveProvider(providerName);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			throw e;
		}
		RuntimeOptions runtimeOptions;
		try {
			runtimeOptions = createRuntimeOptions(command);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			throw e;
		}
		run(command, query, inputFiles, version, jsonProvider, runtimeOptions);
	}

	static RuntimeOptions createRuntimeOptions(CommandLine command) {
		return RuntimeOptions.newBuilder()
				.setMaxStringLength(parseLimit(command, OPT_MAX_STRING_LENGTH))
				.setMaxBinaryLength(parseLimit(command, OPT_MAX_BINARY_LENGTH))
				.setMaxArrayLength(parseLimit(command, OPT_MAX_ARRAY_LENGTH))
				.setMaxObjectMemberCount(parseLimit(command, OPT_MAX_OBJECT_MEMBER_COUNT))
				.setMaxUserDefinedFunctionCalls(parseLongLimit(command, OPT_MAX_USER_DEFINED_FUNCTION_CALLS))
				.setMaxOutputsPerExpression(parseLongLimit(command, OPT_MAX_OUTPUTS_PER_EXPRESSION))
				.build();
	}

	private static int parseLimit(CommandLine command, Option option) {
		String value = command.getOptionValue(option.getLongOpt());
		if (value == null)
			return Integer.MAX_VALUE;
		try {
			int limit = Integer.parseInt(value);
			if (limit < 0)
				throw new NumberFormatException();
			return limit;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid --" + option.getLongOpt() + ": " + value + " (expected a non-negative integer)", e);
		}
	}

	private static long parseLongLimit(CommandLine command, Option option) {
		String value = command.getOptionValue(option.getLongOpt());
		if (value == null)
			return Long.MAX_VALUE;
		try {
			long limit = Long.parseLong(value);
			if (limit < 0)
				throw new NumberFormatException();
			return limit;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid --" + option.getLongOpt() + ": " + value + " (expected a non-negative integer)", e);
		}
	}

	static final List<String> PROVIDERS = List.of("jackson3", "jackson2", "fastjson2", "gson", "jakarta");

	static String resolveProviderName(JsonProvider<?> jsonProvider) {
		if (jsonProvider instanceof Jackson3JsonProvider) {
			return "jackson3";
		}
		if (jsonProvider instanceof Jackson2JsonProvider) {
			return "jackson2";
		}
		if (jsonProvider instanceof Fastjson2JsonProvider) {
			return "fastjson2";
		}
		if (jsonProvider instanceof GsonJsonProvider) {
			return "gson";
		}
		if (jsonProvider instanceof JakartaJsonProvider) {
			return "jakarta";
		}
		return "jackson3";
	}

	static JsonProvider<?> resolveProvider(String name) {
		return switch (name) {
			case "jackson2" -> Jackson2JsonProvider.getInstance();
			case "jackson3" -> Jackson3JsonProvider.getInstance();
			case "fastjson2" -> Fastjson2JsonProvider.getInstance();
			case "gson" -> GsonJsonProvider.getInstance();
			case "jakarta" -> JakartaJsonProvider.getInstance();
			default ->
					throw new IllegalArgumentException("unknown --json-provider: " + name + " (expected one of: jackson2, jackson3, fastjson2, gson, jakarta)");
		};
	}

	/**
	 * Describes why a file could not be opened, in the wording jq inherits from strerror(3).
	 */
	@Nullable
	private static String reason(IOException e) {
		if (e instanceof NoSuchFileException)
			return "No such file or directory";
		if (e instanceof AccessDeniedException)
			return "Permission denied";
		return e.getMessage();
	}

	private static <N> JsonQuery<N> compileOrExit(Environment<N> env, String query, CompileOptions options) {
		try {
			return env.compile(query, options);
		} catch (JsonQueryException e) {
			// The message already carries the position and a caret line; a stack trace on top of it
			// only buries the one line the user needs.
			System.err.println("jq: error: " + e.getMessage());
			System.exit(1);
			throw e;
		}
	}

	static <N> Environment<N> createEnvironment(JsonProvider<N> jsonProvider, Version version) {
		return EnvironmentBuilder.withDefaultLoaders(jsonProvider, version)
				.defineFunction(FunctionSignature.of("env", 0), new Function() {
					@Override
					public <Context extends RuntimeContext, N2> Expression<Context, N2> bind(BindContext<N2> bindCtx, List<Expression<Context, N2>> fnArgs) {
						JsonProvider<N2> jsonProv = bindCtx.getJsonProvider();
						return new Expression<>() {
							@Override
							public Cardinality getCardinality() {
								return Cardinality.ONE;
							}

							@Override
							public boolean dependsOnInput() {
								return false;
							}

							@Override
							public boolean dependsOnExternalState() {
								return true;
							}

							@Override
							public void apply(Context context, N2 in, Path<N2> ipath, Output<N2> output) throws JsonQueryException {
								Map<String, N2> envValues = new HashMap<>();
								for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
									envValues.put(entry.getKey(), jsonProv.createString(entry.getValue()));
								}
								output.emit(jsonProv.createObject(envValues), UntrackedPath.getInstance());
							}
						};
					}
				})
				.addModuleLoader(new FileSystemModuleLoader<>(jsonProvider, FileSystems.getDefault().getPath("").toAbsolutePath()))
				.build();
	}

	private static <N> void run(CommandLine command, String query, List<String> inputFiles, Version version, JsonProvider<N> jsonProvider,
								RuntimeOptions runtimeOptions) throws Exception {
		run(command, query, inputFiles, version, jsonProvider, runtimeOptions, null);
	}

	static <N> void run(CommandLine command, String query, List<String> inputFiles, Version version, JsonProvider<N> jsonProvider,
						RuntimeOptions runtimeOptions, @Nullable TuiRunner customRunner) throws Exception {
		run(command, query, inputFiles, version, jsonProvider, runtimeOptions, customRunner, System.getenv("EDITOR"));
	}

	static <N> void run(CommandLine command, String query, List<String> inputFiles, Version version, JsonProvider<N> jsonProvider,
						RuntimeOptions runtimeOptions, @Nullable TuiRunner customRunner,
						@Nullable String editor) throws Exception {
		Environment<N> env = createEnvironment(jsonProvider, version);
		/*
		 * jq itself emits no warnings at all, so this is purely additive: it goes to stderr, leaving
		 * stdout and the exit code byte-for-byte what jq would produce.
		 */
		CompileOptions.Builder compileOptionsBuilder = CompileOptions.newBuilder();
		if (!command.hasOption(OPT_NO_WARNINGS.getLongOpt()) && !isInteractive(command)) {
			compileOptionsBuilder.setDiagnosticListener(diagnostic -> {
				SourceLocation location = diagnostic.location();
				String excerpt = location != null ? location.excerpt(query) : null;
				System.err.println("jq: warning: " + diagnostic.message()
						+ (location != null ? " at " + location : "")
						+ (excerpt != null ? ":" : ""));
				if (excerpt != null)
					System.err.println(excerpt);
			});
		}
		if (command.hasOption(OPT_DISABLE_TCO.getLongOpt()))
			compileOptionsBuilder.setOptimizationOptions(OptimizationOptions.newBuilder().setTailCallOptimization(false).build());
		CompileOptions compileOptions = compileOptionsBuilder.build();
		boolean compact = command.hasOption(OPT_COMPACT.getOpt());
		boolean rawOutput = command.hasOption(OPT_RAW_OUTPUT.getOpt());
		boolean nullInput = command.hasOption(OPT_NULL_INPUT.getOpt());
		boolean forceColor = command.hasOption(OPT_COLOR_OUTPUT.getOpt());
		boolean forceMonochrome = command.hasOption(OPT_MONOCHROME_OUTPUT.getOpt());
		boolean color;
		if (forceMonochrome) {
			color = false;
		} else if (forceColor) {
			color = true;
		} else {
			String noColor = System.getenv("NO_COLOR");
			boolean noColorSet = noColor != null && !noColor.isEmpty();
			boolean isTty = System.console() != null;
			color = isTty && !noColorSet;
		}
		JqColors colors = color ? JqColors.fromEnvironment(version, System.getenv(), System.err) : null;
		List<InputStream> streams = new ArrayList<>();
		@Var boolean failed = false;
		/*
		 * jq still reads input files with --null-input so that input/inputs can consume them, but
		 * jackson-jq has no such builtin. Neither stdin nor input files are opened or read here.
		 */
		if (!nullInput) {
			if (inputFiles.isEmpty()) {
				streams.add(System.in);
			} else {
				// jq reports the files it cannot open, processes the remaining ones and fails at the end.
				for (String inputFile : inputFiles) {
					try {
						streams.add(openInput(inputFile));
					} catch (IOException e) {
						System.err.println("jq: error: Could not open file " + inputFile + ": " + reason(e));
						failed = true;
					}
				}
			}
		}
		if (isInteractive(command)) {
			if (failed)
				System.exit(1);
			@Var byte[] rawInputBytes = null;
			if (!nullInput) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[8192];
				for (InputStream s : streams) {
					try {
						@Var int n;
						while ((n = s.read(buf)) != -1) {
							baos.write(buf, 0, n);
						}
					} catch (IOException e) {
						System.err.println("jq: error: Could not read input: " + reason(e));
						System.exit(1);
					}
				}
				rawInputBytes = baos.toByteArray();
			}
			TuiRunner runner;
			if (customRunner != null) {
				runner = customRunner;
			} else {
				try {
					runner = createDefaultRunner();
				} catch (Exception e) {
					System.err.println("jackson-jq: error: --interactive requires an interactive terminal: " + e.getMessage());
					System.exit(1);
					throw e;
				}
			}
			boolean rawInput = command.hasOption(OPT_RAW_INPUT.getOpt());
			boolean slurp = command.hasOption(OPT_SLURP.getOpt());
			boolean warningsEnabled = !command.hasOption(OPT_NO_WARNINGS.getLongOpt());
			String providerName = resolveProviderName(jsonProvider);
			ExecutorService evalExecutor = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "jq-playground-eval");
				t.setDaemon(true);
				return t;
			});
			Playground<N> pg = new Playground<>(env, version, providerName, rawInputBytes, nullInput, rawInput, slurp,
					query, jsonProvider, runtimeOptions, compileOptions, compact, rawOutput, warningsEnabled, inputFiles,
					isVimMode(command, editor), System.out, System.err);
			pg.setEvaluationExecutor(evalExecutor);
			try {
				pg.run(runner);
			} finally {
				evalExecutor.shutdownNow();
			}
			return;
		}
		InputSource<N> input = InputSources.create(jsonProvider, streams, nullInput,
				command.hasOption(OPT_RAW_INPUT.getOpt()),
				command.hasOption(OPT_SLURP.getOpt()));
		JsonQuery<N> jq = compileOrExit(env, query, compileOptions).withRuntimeOptions(runtimeOptions);
		input.readAll(tree -> {
			try {
				jq.apply(tree, out -> {
					if (jsonProvider.isString(out) && rawOutput) {
						System.out.println(jsonProvider.getString(out));
					} else if (compact) {
						System.out.println(JqPrinter.print(jsonProvider, out, null, colors));
					} else {
						System.out.println(JqPrinter.print(jsonProvider, out, PRETTY_INDENT, colors));
					}
				});
			} catch (JsonQueryException e) {
				System.err.println("jq: error: " + e.getMessage());
				System.exit(1);
			}
		});
		if (failed)
			System.exit(1);
	}

	static TuiRunner createDefaultRunner() throws Exception {
		if (System.console() != null) {
			try {
				return TuiRunner.create();
			} catch (Exception ignored) {
				// Fall through to /dev/tty fallback
			}
		}
		if (!OSUtils.IS_WINDOWS) {
			File devTty = new File("/dev/tty");
			if (devTty.exists()) {
				ExecTerminalProvider provider = new ExecTerminalProvider();
				DevTtyPty pty = new DevTtyPty(provider);
				String termEnv = System.getenv("TERM");
				String termType = (termEnv != null && !termEnv.isEmpty()) ? termEnv : "xterm-256color";
				Terminal terminal = new PosixSysTerminal(
						"jackson-jq", termType, pty, StandardCharsets.UTF_8, false, Terminal.SignalHandler.SIG_DFL);
				JLineBackend backend = new JLineBackend(terminal);
				TuiConfig config = TuiConfig.builder().backend(backend).build();
				return TuiRunner.create(config);
			}
		}
		return TuiRunner.create();
	}

	static boolean isInteractive(CommandLine command) {
		VimSetting vimSetting = resolveVimSetting(command);
		return command.hasOption(OPT_INTERACTIVE.getLongOpt()) || vimSetting == VimSetting.ENABLED;
	}

	static boolean isVimMode(CommandLine command, @Nullable String editor) {
		return switch (resolveVimSetting(command)) {
			case ENABLED -> true;
			case DISABLED -> false;
			case AUTO -> isVimEditor(editor);
		};
	}

	private static VimSetting resolveVimSetting(CommandLine command) {
		String value = command.getOptionValue(OPT_VIM.getLongOpt());
		if (value == null) {
			return VimSetting.AUTO;
		}
		return switch (value) {
			case "auto" -> VimSetting.AUTO;
			case "true" -> VimSetting.ENABLED;
			case "false" -> VimSetting.DISABLED;
			default -> throw new IllegalArgumentException(
					"invalid --vim: " + value + " (expected one of: auto, true, false)");
		};
	}

	static boolean isVimEditor(@Nullable String editor) {
		if (editor == null) {
			return false;
		}
		String command = editor.trim();
		if (command.isEmpty()) {
			return false;
		}
		String executable;
		char first = command.charAt(0);
		if (first == '\'' || first == '"') {
			int closingQuote = command.indexOf(first, 1);
			executable = closingQuote < 0 ? command.substring(1) : command.substring(1, closingQuote);
		} else {
			@Var int end = 0;
			while (end < command.length() && !Character.isWhitespace(command.charAt(end))) {
				end++;
			}
			executable = command.substring(0, end);
		}
		int slash = Math.max(executable.lastIndexOf('/'), executable.lastIndexOf('\\'));
		String basename = executable.substring(slash + 1);
		return basename.equals("vi") || basename.equals("vim");
	}

	private static final class DevTtyPty extends ExecPty {
		private DevTtyPty(TerminalProvider provider) {
			super(provider, null, "/dev/tty");
		}
	}

	private static InputStream openInput(String inputFile) throws IOException {
		if (STDIN_FILE_NAME.equals(inputFile))
			return System.in;
		return Files.newInputStream(Paths.get(inputFile));
	}
}
