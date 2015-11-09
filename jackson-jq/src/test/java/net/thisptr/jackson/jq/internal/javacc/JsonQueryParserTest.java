package net.thisptr.jackson.jq.internal.javacc;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;

import org.junit.Test;

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
	public void testSupportedQueries() throws IOException, ParseException, TokenMgrError {
		final List<String> loadQueries = loadQueries("compiler-test-ok.txt");
		for (int i = 0; i < loadQueries.size(); i++) {
			final JsonQuery jq = JsonQueryParser.compile(loadQueries.get(i));
			if (jq == null) {
				System.out.printf("%d: ---%n", i);
			} else {
				System.out.printf("%d: %s%n", i, jq);
			}
		}
	}

	@Test
	public void testUnsupportedQueries() throws IOException {
		final List<String> loadQueries = loadQueries("compiler-test-ng.txt");
		for (int i = 0; i < loadQueries.size(); i++) {
			try {
				JsonQueryParser.compile(loadQueries.get(i));
			} catch (final ParseException | TokenMgrError e) {
				continue;
			}
			fail("should throw ParseException");
		}
	}
}
