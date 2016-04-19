package net.thisptr.jackson.jq.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Main {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Option OPT_COMPACT = Option.builder("c")
			.longOpt("compact")
			.desc("compact instead of pretty-printed output")
			.build();

	private static final Option OPT_RAW = Option.builder("r")
			.longOpt("raw")
			.desc("output raw strings, not JSON texts")
			.build();

	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.build();

	public static void main(String[] args) throws IOException, ParseException {
		final CommandLine command;
		final JsonQuery jq;
		try {
			final CommandLineParser parser = new DefaultParser();
			final Options options = new Options();
			options.addOption(OPT_COMPACT);
			options.addOption(OPT_RAW);
			options.addOption(OPT_HELP);
			command = parser.parse(options, args);
			final List<String> rest = command.getArgList();
			if (rest.isEmpty() || command.hasOption(OPT_HELP.getOpt())) {
				final HelpFormatter help = new HelpFormatter();
				help.printHelp("jackson-jq [OPTIONS...] QUERY", options, false);
				System.exit(0);
			}
			jq = JsonQuery.compile(rest.get(0));
		} catch (ParseException e) {
			System.err.println("invalid arguments: " + Arrays.toString(args));
			System.exit(1);
			throw e;
		}

		if (!command.hasOption(OPT_COMPACT.getOpt())) {
			MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
		}

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			final JsonParser parser = MAPPER.getFactory().createParser(reader);
			while (!parser.isClosed()) {
				final JsonNode tree = parser.readValueAsTree();
				if (tree == null)
					continue;
				try {
					for (final JsonNode out : jq.apply(tree)) {
						if (out.isTextual() && command.hasOption(OPT_RAW.getOpt())) {
							System.out.println(out.asText());
						} else {
							System.out.println(MAPPER.writeValueAsString(out));
						}
					}
				} catch (JsonQueryException e) {
					System.err.println("jq: error: " + e.getMessage());
					System.exit(1);
				}
			}
		}
	}
}
