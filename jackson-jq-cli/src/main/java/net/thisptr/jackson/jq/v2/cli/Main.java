package net.thisptr.jackson.jq.v2.cli;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class Main {
	private static ObjectMapper MAPPER = JsonMapper.builder()
			.addModule(JsonQueryJacksonModule.getInstance())
			.build();

	private static final Option OPT_COMPACT = Option.builder("c")
			.longOpt("compact")
			.desc("compact instead of pretty-printed output")
			.build();

	private static final Option OPT_RAW_OUTPUT = Option.builder("r")
			.longOpt("raw-output")
			.desc("output raw strings, not JSON texts")
			.build();

	private static final Option OPT_NULL_INPUT = Option.builder("n")
			.longOpt("null-input")
			.desc("use `null` as the single input value")
			.build();

	private static final Option OPT_VERSION = Option.builder()
			.longOpt("jq")
			.desc("specify jq version")
			.numberOfArgs(1)
			.build();

	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.build();

	public static void main(String[] args) throws IOException, ParseException {
		Options options = new Options();
		options.addOption(OPT_COMPACT);
		options.addOption(OPT_RAW_OUTPUT);
		options.addOption(OPT_NULL_INPUT);
		options.addOption(OPT_VERSION);
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

		if (rest.isEmpty() || command.hasOption(OPT_HELP.getOpt())) {
			HelpFormatter help = new HelpFormatter();
			help.printHelp("jackson-jq [OPTIONS...] QUERY", options, false);
			System.exit(0);
		}

		Jackson3JsonProviderImpl jsonProvider = Jackson3JsonProviderImpl.getInstance();
		Environment<JsonNode> env = new Environment<>(jsonProvider, version);
		env.addFunction(FunctionNameAndArity.of("env", 0), new Function() {
			@Override
			public <N> Expression<N> bindArguments(JsonProvider<N> jsonProv, List<Expression<N>> fnArgs, Version ver) {
				return (frame, in, path, output, ignoredRequirePath) -> {
					N envObj = jsonProv.createObject();
					for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
						jsonProv.set(envObj, entry.getKey(), jsonProv.createString(entry.getValue()));
					}
					output.emit(envObj, null);
				};
			}
		});
		env.setModuleLoader(new ChainedModuleLoader<>(new ModuleLoader[] {
				ClassPathModuleLoader.getInstance(),
				new FileSystemModuleLoader<>(jsonProvider, version, FileSystems.getDefault().getPath("").toAbsolutePath()),
		}));

		JsonQuery<JsonNode> jq = env.compile(rest.get(0));

		if (!command.hasOption(OPT_COMPACT.getOpt())) {
			MAPPER = MAPPER.rebuild()
					.enable(SerializationFeature.INDENT_OUTPUT)
					.build();
		}

		@Var InputStream is = System.in;
		if (command.hasOption(OPT_NULL_INPUT.getOpt())) {
			is = new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8));
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			 MappingIterator<JsonNode> iter = MAPPER.readerFor(JsonNode.class).readValues(reader)) {
			while (iter.hasNext()) {
				JsonNode tree = iter.next();
				try {
					jq.apply(tree, (out, path) -> {
						if (out.isTextual() && command.hasOption(OPT_RAW_OUTPUT.getOpt())) {
							System.out.println(out.asString());
						} else {
							System.out.println(MAPPER.writeValueAsString(out));
						}
					});
				} catch (JsonQueryException e) {
					System.err.println("jq: error: " + e.getMessage());
					System.exit(1);
				}
			}
		}
	}
}
