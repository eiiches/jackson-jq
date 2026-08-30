package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.v2.json.impl.jakarta.JakartaJsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class Main {
	private static final ObjectMapper MAPPER = JsonMapper.builder()
			.addModule(JsonQueryJacksonModule.getInstance())
			.build();
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
	private static final Option OPT_VERSION = Option.builder()
			.longOpt("jq")
			.desc("specify jq version")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_JSON_PROVIDER = Option.builder()
			.longOpt("json-provider")
			.desc("JSON provider: jackson2, jackson3, gson, or jakarta (default: jackson3)")
			.numberOfArgs(1)
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
		options.addOption(OPT_VERSION);
		options.addOption(OPT_JSON_PROVIDER);
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
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("jackson-jq [OPTIONS...] QUERY", null, options, null, false);
			System.exit(0);
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
		run(command, rest.get(0), version, jsonProvider);
	}

	static JsonProvider<?> resolveProvider(String name) {
		switch (name) {
			case "jackson2":
				return Jackson2JsonProviderImpl.getInstance();
			case "jackson3":
				return Jackson3JsonProviderImpl.getInstance();
			case "gson":
				return GsonJsonProviderImpl.getInstance();
			case "jakarta":
				return JakartaJsonProviderImpl.getInstance();
			default:
				throw new IllegalArgumentException("unknown --json-provider: " + name + " (expected one of: jackson2, jackson3, gson, jakarta)");
		}
	}

	private static <N> void run(CommandLine command, String query, Version version, JsonProvider<N> jsonProvider) throws Exception {
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
		JsonQuery<N> jq = env.compile(query);
		// TODO: Pretty-printing still goes through Jackson 3 regardless of --json-provider. Drop MAPPER
		// once JsonProvider can format to a stream.
		ObjectMapper outputMapper = command.hasOption(OPT_COMPACT.getOpt())
				? MAPPER
				: MAPPER.rebuild()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.build();
		@Var InputStream is = System.in;
		if (command.hasOption(OPT_NULL_INPUT.getOpt())) {
			is = new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8));
		}
		try (JsonParser<N> parser = jsonProvider.createParser(is)) {
			for (@Var N tree = parser.next(); tree != null; tree = parser.next()) {
				try {
					jq.apply(tree, (out, path) -> {
						if (jsonProvider.getNodeType(out) == JsonNodeType.STRING && command.hasOption(OPT_RAW_OUTPUT.getOpt())) {
							System.out.println(jsonProvider.asString(out));
						} else {
							String json = jsonProvider.format(out);
							if (command.hasOption(OPT_COMPACT.getOpt())) {
								System.out.println(json);
							} else {
								JsonNode outputTree = MAPPER.readTree(json);
								System.out.println(outputMapper.writeValueAsString(outputTree));
							}
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
