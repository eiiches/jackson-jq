package net.thisptr.jackson.jq.v2.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.fastjson2.Fastjson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class Main {
	static final String QUERY_PROPERTY = "jackson.jq.benchmark.query";
	static final String INPUT_PROPERTY = "jackson.jq.benchmark.input";

	private static final String DEFAULT_JSON_PROVIDER = "jackson3";
	private static final String DEFAULT_JQ_VERSION = "1.8.2";
	private static final Pattern BENCHMARK_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

	public static void main(String[] args) throws Exception {
		Configuration configuration;
		try {
			configuration = parseArguments(args);
			if (!configuration.help()) {
				// Validate these in the launcher as well as in forked benchmark workers.
				resolveProvider(configuration.jsonProviderName());
				resolveVersion(configuration.jqVersion());
			}
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(1);
			return;
		}
		if (configuration.help()) {
			printUsage();
			return;
		}

		String previousQuery = System.setProperty(QUERY_PROPERTY, configuration.jqExpression());
		String previousInput = System.setProperty(INPUT_PROPERTY, configuration.jsonInput());
		try {
			Options jmhCommandLine = new CommandLineOptions(configuration.jmhArguments());
			List<String> jvmArgsAppend = new ArrayList<>(jmhCommandLine.getJvmArgsAppend().orElse(List.of()));
			jvmArgsAppend.add(systemPropertyArgument(QUERY_PROPERTY, configuration.jqExpression()));
			jvmArgsAppend.add(systemPropertyArgument(INPUT_PROPERTY, configuration.jsonInput()));
			Options options = new OptionsBuilder()
					.parent(jmhCommandLine)
					.include("^" + Pattern.quote(JacksonJqBenchmark.class.getName()) + "\\.")
					.param("benchmarkId", configuration.benchmarkId())
					.param("jsonProviderName", configuration.jsonProviderName())
					.param("jqVersion", configuration.jqVersion())
					.jvmArgsAppend(jvmArgsAppend.toArray(String[]::new))
					.shouldFailOnError(true)
					.build();
			new Runner(options).run();
		} finally {
			restoreProperty(QUERY_PROPERTY, previousQuery);
			restoreProperty(INPUT_PROPERTY, previousInput);
		}
	}

	private static String systemPropertyArgument(String name, String value) {
		return "-D" + name + "=" + value;
	}

	private static void restoreProperty(String name, String previousValue) {
		if (previousValue == null)
			System.clearProperty(name);
		else
			System.setProperty(name, previousValue);
	}

	private static Configuration parseArguments(String[] args) {
		@Var String benchmarkId = null;
		@Var String jsonProviderName = DEFAULT_JSON_PROVIDER;
		@Var String jqVersion = DEFAULT_JQ_VERSION;
		@Var int index = 0;
		while (index < args.length) {
			String argument = args[index];
			if ("--help".equals(argument))
				return Configuration.forHelp();
			if ("--".equals(argument)) {
				index++;
				break;
			}
			if ("--benchmark-id".equals(argument)) {
				benchmarkId = validateBenchmarkId(optionValue(args, ++index, argument));
				index++;
				continue;
			}
			if (argument.startsWith("--benchmark-id=")) {
				benchmarkId = validateBenchmarkId(inlineOptionValue(argument, "--benchmark-id="));
				index++;
				continue;
			}
			if ("--json-provider".equals(argument)) {
				jsonProviderName = optionValue(args, ++index, argument);
				index++;
				continue;
			}
			if (argument.startsWith("--json-provider=")) {
				jsonProviderName = inlineOptionValue(argument, "--json-provider=");
				index++;
				continue;
			}
			if ("--jq-version".equals(argument)) {
				jqVersion = optionValue(args, ++index, argument);
				index++;
				continue;
			}
			if (argument.startsWith("--jq-version=")) {
				jqVersion = inlineOptionValue(argument, "--jq-version=");
				index++;
				continue;
			}
			if (argument.startsWith("-"))
				throw new IllegalArgumentException("unknown benchmark option: " + argument);
			break;
		}

		if (args.length - index < 2)
			throw new IllegalArgumentException("a jq expression and JSON input are required");
		String jqExpression = args[index++];
		String jsonInput = args[index++];
		String[] jmhArguments = Arrays.copyOfRange(args, index, args.length);
		String timestamp = Long.toString(System.currentTimeMillis());
		String effectiveBenchmarkId = benchmarkId == null ? timestamp : benchmarkId + "-" + timestamp;
		return new Configuration(effectiveBenchmarkId, jsonProviderName, jqVersion, jqExpression, jsonInput, jmhArguments, false);
	}

	private static String validateBenchmarkId(String value) {
		if (!BENCHMARK_ID_PATTERN.matcher(value).matches())
			throw new IllegalArgumentException("invalid --benchmark-id: " + value
					+ " (expected 1-64 ASCII letters, digits, dots, underscores, or hyphens; the first character must be alphanumeric)");
		return value;
	}

	private static String optionValue(String[] args, int index, String option) {
		if (index >= args.length || args[index].isEmpty())
			throw new IllegalArgumentException("missing value for " + option);
		return args[index];
	}

	private static String inlineOptionValue(String argument, String prefix) {
		String value = argument.substring(prefix.length());
		if (value.isEmpty())
			throw new IllegalArgumentException("missing value for " + prefix.substring(0, prefix.length() - 1));
		return value;
	}

	// Every returned provider is used only with JSON nodes created by that same provider.
	@SuppressWarnings("unchecked")
	static JsonProvider<Object> resolveProvider(String name) {
		switch (name) {
			case "jackson2":
				return (JsonProvider<Object>) (JsonProvider<?>) Jackson2JsonProvider.getInstance();
			case "jackson3":
				return (JsonProvider<Object>) (JsonProvider<?>) Jackson3JsonProvider.getInstance();
			case "fastjson2":
				return (JsonProvider<Object>) (JsonProvider<?>) Fastjson2JsonProvider.getInstance();
			case "gson":
				return (JsonProvider<Object>) (JsonProvider<?>) GsonJsonProvider.getInstance();
			case "jakarta":
				return (JsonProvider<Object>) (JsonProvider<?>) JakartaJsonProvider.getInstance();
			default:
				throw new IllegalArgumentException("unknown --json-provider: " + name
						+ " (expected one of: jackson2, jackson3, fastjson2, gson, jakarta)");
		}
	}

	static Version resolveVersion(String value) {
		Version version = Version.valueOf(value);
		if (!Versions.versions().contains(version))
			throw new IllegalArgumentException("unsupported --jq-version: " + value + " (expected one of: "
					+ Versions.versions() + ")");
		return version;
	}

	private static void printUsage() {
		System.err.println("Usage: jackson-jq-benchmark [OPTIONS] QUERY JSON [JMH_OPTIONS...]");
		System.err.println("  --benchmark-id NAME   short run label; Unix milliseconds are appended (default: Unix milliseconds)");
		System.err.println("  --json-provider NAME  jackson2, jackson3, fastjson2, gson, or jakarta (default: jackson3)");
		System.err.println("  --jq-version VERSION  jq compatibility version (default: 1.8.2)");
		System.err.println("  --help                show this help");
		System.err.println("Use -- before QUERY when the jq expression starts with '-'.");
		System.err.println("Arguments after JSON are passed to JMH; use -h there for JMH help.");
	}

	private record Configuration(
			String benchmarkId,
			String jsonProviderName,
			String jqVersion,
			String jqExpression,
			String jsonInput,
			String[] jmhArguments,
			boolean help) {
		private static Configuration forHelp() {
			return new Configuration("", "", "", "", "", new String[0], true);
		}
	}

	private Main() {
	}
}
