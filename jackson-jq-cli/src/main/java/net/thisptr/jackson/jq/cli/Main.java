package net.thisptr.jackson.jq.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: jackson-jq QUERY");
			System.exit(1);
		}
		final JsonQuery q = JsonQuery.compile(args[0]);
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				if (line.isEmpty())
					continue;
				try {
					final JsonNode in = MAPPER.readTree(line);
					for (final JsonNode out : q.apply(in)) {
						System.out.println(out);
						System.out.flush();
					}
				} catch (JsonQueryException e) {
					System.err.println("jq: error: " + e.getMessage());
				}
			}
		}
	}
}
