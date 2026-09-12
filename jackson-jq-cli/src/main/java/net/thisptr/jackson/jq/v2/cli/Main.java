package net.thisptr.jackson.jq.v2.cli;

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

import com.google.errorprone.annotations.Var;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.fastjson2.Fastjson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Main {
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
	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.get();

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(OPT_COMPACT);
		options.addOption(OPT_RAW_OUTPUT);
		options.addOption(OPT_NULL_INPUT);
		options.addOption(OPT_RAW_INPUT);
		options.addOption(OPT_SLURP);
		options.addOption(OPT_FROM_FILE);
		options.addOption(OPT_VERSION);
		options.addOption(OPT_JSON_PROVIDER);
		options.addOption(OPT_NO_WARNINGS);
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
		@Var Version version = Versions.JQ_1_6;
		if (command.hasOption(OPT_VERSION.getLongOpt())) {
			version = Version.valueOf(command.getOptionValue(OPT_VERSION.getLongOpt()));
			if (!Versions.versions().contains(version)) {
				System.err.println("unsupported --jq version: " + version);
				System.exit(1);
			}
		}
		String queryFile = command.getOptionValue(OPT_FROM_FILE.getOpt());
		if ((queryFile == null && rest.isEmpty()) || command.hasOption(OPT_HELP.getOpt())) {
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("jackson-jq [OPTIONS...] QUERY [FILE...]", null, options, null, false);
			System.exit(0);
		}
		String query;
		List<String> inputFiles;
		if (queryFile != null) {
			// jq reads the query from the file, so that every positional argument is an input file.
			try {
				query = new String(Files.readAllBytes(Paths.get(queryFile)), StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.err.println("jq: error: Could not open " + queryFile + ": " + reason(e));
				System.exit(1);
				throw e;
			}
			inputFiles = rest;
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
		run(command, query, inputFiles, version, jsonProvider);
	}

	static JsonProvider<?> resolveProvider(String name) {
		switch (name) {
			case "jackson2":
				return Jackson2JsonProviderImpl.getInstance();
			case "jackson3":
				return Jackson3JsonProviderImpl.getInstance();
			case "fastjson2":
				return Fastjson2JsonProviderImpl.getInstance();
			case "gson":
				return GsonJsonProviderImpl.getInstance();
			case "jakarta":
				return JakartaJsonProviderImpl.getInstance();
			default:
				throw new IllegalArgumentException("unknown --json-provider: " + name + " (expected one of: jackson2, jackson3, fastjson2, gson, jakarta)");
		}
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

	private static <N> void run(CommandLine command, String query, List<String> inputFiles, Version version, JsonProvider<N> jsonProvider) throws Exception {
		Environment<N> env = new EnvironmentBuilder<>(jsonProvider, version)
				.defineFunction(FunctionSignature.of("env", 0), new Function() {
					@Override
					public <Context, N2> Expression<Context, N2> bindArguments(JsonProvider<N2> jsonProv, List<Expression<Context, N2>> fnArgs, Version ver) {
						return new Expression<Context, N2>() {
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
				.setModuleLoader(new ChainedModuleLoader<N>(
						ClassPathModuleLoader.getInstance(),
						new FileSystemModuleLoader<>(jsonProvider, version, FileSystems.getDefault().getPath("").toAbsolutePath())))
				.build();
		/*
		 * jq itself emits no warnings at all, so this is purely additive: it goes to stderr, leaving
		 * stdout and the exit code byte-for-byte what jq would produce.
		 */
		CompileOptions compileOptions = new CompileOptions();
		if (!command.hasOption(OPT_NO_WARNINGS.getLongOpt())) {
			compileOptions.setDiagnosticListener(diagnostic -> {
				SourceLocation location = diagnostic.location();
				String excerpt = location != null ? location.excerpt(query) : null;
				System.err.println("jq: warning: " + diagnostic.message()
						+ (location != null ? " at " + location : "")
						+ (excerpt != null ? ":" : ""));
				if (excerpt != null)
					System.err.println(excerpt);
			});
		}
		JsonQuery<N> jq = compileOrExit(env, query, compileOptions);
		boolean compact = command.hasOption(OPT_COMPACT.getOpt());
		boolean rawOutput = command.hasOption(OPT_RAW_OUTPUT.getOpt());
		boolean nullInput = command.hasOption(OPT_NULL_INPUT.getOpt());
		List<InputStream> streams = new ArrayList<>();
		@Var boolean failed = false;
		/*
		 * jq still reads the input files with --null-input, so that input/inputs can consume them,
		 * but jackson-jq has no such builtin and the files are simply left unread.
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
		InputSource<N> input = InputSources.create(jsonProvider, streams, nullInput,
				command.hasOption(OPT_RAW_INPUT.getOpt()),
				command.hasOption(OPT_SLURP.getOpt()));
		input.readAll(tree -> {
			try {
				jq.apply(tree, out -> {
					if (jsonProvider.isString(out) && rawOutput) {
						System.out.println(jsonProvider.getString(out));
					} else if (compact) {
						System.out.println(jsonProvider.format(out));
					} else {
						System.out.println(JqPrettyPrinter.print(jsonProvider, out, PRETTY_INDENT));
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

	private static InputStream openInput(String inputFile) throws IOException {
		if (STDIN_FILE_NAME.equals(inputFile))
			return System.in;
		return Files.newInputStream(Paths.get(inputFile));
	}
}
