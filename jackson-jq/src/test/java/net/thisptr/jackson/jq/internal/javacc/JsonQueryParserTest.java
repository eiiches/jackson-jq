package net.thisptr.jackson.jq.internal.javacc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JsonQueryParserTest {
	private static List<String> loadQueries(final String fname) throws IOException, TokenMgrError {
		final List<String> result = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(JsonQueryParserTest.class.getClassLoader().getResourceAsStream(fname)))) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;

				if (line.isEmpty())
					continue;
				if (line.startsWith("#"))
					continue;

				result.add(line);
			}
		}
		return result;
	}

	@Test
	public void testSupportedQueries() throws IOException, TokenMgrError {
		final List<String> loadQueries = loadQueries("compiler-test-ok.txt");
		for (int i = 0; i < loadQueries.size(); i++) {
			try {
				final Expression jq = ExpressionParser.compile(loadQueries.get(i), Versions.JQ_1_5);
				if (jq == null) {
					System.out.printf("%d: ---%n", i);
				} else {
					System.out.printf("%d: %s%n", i, jq);
				}
			} catch (final Throwable th) {
				throw new RuntimeException("Failed to compile \"" + loadQueries.get(i) + "\"", th);
			}
		}
	}

	@Test
	public void testUnsupportedQueries() throws IOException {
		final List<String> loadQueries = loadQueries("compiler-test-ng.txt");
		for (String q : loadQueries) {
			assertThrows(JsonQueryException.class, () -> ExpressionParser.compile(q, Versions.JQ_1_5));
		}
	}
}
