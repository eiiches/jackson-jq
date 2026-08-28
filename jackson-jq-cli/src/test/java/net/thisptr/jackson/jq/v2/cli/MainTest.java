package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MainTest {
	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void evaluatesWithSelectedJsonProvider(String provider) throws Exception {
		assertThat(run("{\"foo\":41}", "--json-provider", provider, "--compact", ".foo + 1"))
				.isEqualTo("42\n");
	}

	@Test
	void defaultsToJackson3() throws Exception {
		assertThat(run("{\"foo\":41}", "--compact", ".foo + 1"))
				.isEqualTo("42\n");
	}

	@Test
	void supportsRawOutputWithSelectedJsonProvider() throws Exception {
		assertThat(run("null", "--json-provider", "gson", "--raw-output", "\"hello\""))
				.isEqualTo("hello\n");
	}

	@Test
	void supportsNullInputWithSelectedJsonProvider() throws Exception {
		assertThat(run("ignored", "--json-provider", "jackson2", "--null-input", "--compact", ". == null"))
				.isEqualTo("true\n");
	}

	@Test
	void rejectsUnknownJsonProvider() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.resolveProvider("unknown"))
				.withMessage("unknown --json-provider: unknown (expected one of: jackson2, jackson3, gson, jakarta)");
	}

	private static synchronized String run(String input, String... args) throws Exception {
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(output));
			Main.main(args);
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
		}
	}
}
