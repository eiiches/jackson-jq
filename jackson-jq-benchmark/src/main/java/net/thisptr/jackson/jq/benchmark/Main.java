package net.thisptr.jackson.jq.benchmark;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;

public class Main {
	private static final String DEFAULT_JQ_VERSION = "1.7";

	public static void main(final String[] args) throws Exception {
		final Configuration configuration;
		try {
			configuration = parseArguments(args);
			if (!configuration.help)
				resolveVersion(configuration.jqVersion);
		} catch (final IllegalArgumentException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(1);
			return;
		}
		if (configuration.help) {
			printUsage();
			return;
		}

		final Options jmhCommandLine = new CommandLineOptions(configuration.jmhArguments);
		final Options options = new OptionsBuilder()
				.parent(jmhCommandLine)
				.include("^" + Pattern.quote(JacksonJqBenchmark.class.getName()) + "\\.")
				.param("jqExpression", configuration.jqExpression)
				.param("jsonInput", configuration.jsonInput)
				.param("jqVersion", configuration.jqVersion)
				.shouldFailOnError(true)
				.build();
		new Runner(options).run();
	}

	private static Configuration parseArguments(final String[] args) {
		String jqVersion = DEFAULT_JQ_VERSION;
		int index = 0;
		while (index < args.length) {
			final String argument = args[index];
			if ("--help".equals(argument))
				return Configuration.help();
			if ("--".equals(argument)) {
				index++;
				break;
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
		final String jqExpression = args[index++];
		final String jsonInput = args[index++];
		final String[] jmhArguments = Arrays.copyOfRange(args, index, args.length);
		return new Configuration(jqVersion, jqExpression, jsonInput, jmhArguments, false);
	}

	private static String optionValue(final String[] args, final int index, final String option) {
		if (index >= args.length || args[index].isEmpty())
			throw new IllegalArgumentException("missing value for " + option);
		return args[index];
	}

	private static String inlineOptionValue(final String argument, final String prefix) {
		final String value = argument.substring(prefix.length());
		if (value.isEmpty())
			throw new IllegalArgumentException("missing value for " + prefix.substring(0, prefix.length() - 1));
		return value;
	}

	static Version resolveVersion(final String value) {
		final Version version = Version.valueOf(value);
		if (!Versions.versions().contains(version))
			throw new IllegalArgumentException("unsupported --jq-version: " + value + " (expected one of: "
					+ Versions.versions() + ")");
		return version;
	}

	private static void printUsage() {
		System.err.println("Usage: java -jar benchmarks.jar [OPTIONS] QUERY JSON [JMH_OPTIONS...]");
		System.err.println("  --jq-version VERSION  jq compatibility version (default: 1.7)");
		System.err.println("  --help                show this help");
		System.err.println("Use -- before QUERY when the jq expression starts with '-'.");
		System.err.println("Arguments after JSON are passed to JMH; use -h there for JMH help.");
	}

	private static class Configuration {
		private final String jqVersion;
		private final String jqExpression;
		private final String jsonInput;
		private final String[] jmhArguments;
		private final boolean help;

		private Configuration(final String jqVersion, final String jqExpression, final String jsonInput, final String[] jmhArguments, final boolean help) {
			this.jqVersion = jqVersion;
			this.jqExpression = jqExpression;
			this.jsonInput = jsonInput;
			this.jmhArguments = jmhArguments;
			this.help = help;
		}

		private static Configuration help() {
			return new Configuration("", "", "", new String[0], true);
		}
	}

	private Main() {
	}
}
