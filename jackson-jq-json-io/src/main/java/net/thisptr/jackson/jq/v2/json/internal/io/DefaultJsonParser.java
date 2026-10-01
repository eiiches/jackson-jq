package net.thisptr.jackson.jq.v2.json.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;

final class DefaultJsonParser<N> implements JsonParser<N> {
	private final JsonProvider<N> provider;
	private final PushbackReader reader;
	private boolean exhausted;

	DefaultJsonParser(JsonProvider<N> provider, InputStream in) {
		this.provider = provider;
		this.reader = new PushbackReader(new InputStreamReader(in, StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)), 1);
	}

	@Override
	public Maybe<N> next() {
		if (exhausted)
			return Maybe.absent();
		try {
			int ch = nextNonWhitespace();
			if (ch < 0) {
				exhausted = true;
				return Maybe.absent();
			}
			return Maybe.of(readValue(ch));
		} catch (IOException | NumberFormatException e) {
			throw new JsonException(e);
		}
	}

	private N readValue(int ch) throws IOException {
		return switch (ch) {
			case '{' -> readObject();
			case '[' -> readArray();
			case '"' -> provider.createString(readString());
			case 't' -> {
				readLiteral("rue");
				yield provider.createBoolean(true);
			}
			case 'f' -> {
				readLiteral("alse");
				yield provider.createBoolean(false);
			}
			case 'n' -> {
				readLiteral("ull");
				yield provider.createNull();
			}
			default -> {
				if (ch == '-' || isDigit(ch))
					yield readNumber(ch);
				throw invalid("Unexpected character: " + (char) ch);
			}
		};
	}

	private N readObject() throws IOException {
		Map<String, N> members = new LinkedHashMap<>();
		@Var int ch = nextNonWhitespace();
		if (ch == '}')
			return provider.createObject(members);
		while (true) {
			if (ch != '"')
				throw invalid("Expected object key");
			String key = readString();
			if (nextNonWhitespace() != ':')
				throw invalid("Expected ':' after object key");
			members.put(key, readValue(nextNonWhitespace()));
			ch = nextNonWhitespace();
			if (ch == '}')
				return provider.createObject(members);
			if (ch != ',')
				throw invalid("Expected ',' or '}'");
			ch = nextNonWhitespace();
		}
	}

	private N readArray() throws IOException {
		List<N> values = new ArrayList<>();
		@Var int ch = nextNonWhitespace();
		if (ch == ']')
			return provider.createArray(values);
		while (true) {
			values.add(readValue(ch));
			ch = nextNonWhitespace();
			if (ch == ']')
				return provider.createArray(values);
			if (ch != ',')
				throw invalid("Expected ',' or ']'");
			ch = nextNonWhitespace();
		}
	}

	private String readString() throws IOException {
		StringBuilder value = new StringBuilder();
		for (int ch = reader.read(); ch >= 0; ch = reader.read()) {
			if (ch == '"')
				return value.toString();
			if (ch < 0x20)
				throw invalid("Unescaped control character in string");
			if (ch != '\\') {
				value.append((char) ch);
				continue;
			}
			ch = reader.read();
			switch (ch) {
				case '"', '\\', '/' -> value.append((char) ch);
				case 'b' -> value.append('\b');
				case 'f' -> value.append('\f');
				case 'n' -> value.append('\n');
				case 'r' -> value.append('\r');
				case 't' -> value.append('\t');
				case 'u' -> value.append((char) readHexCodeUnit());
				default -> throw invalid("Invalid string escape");
			}
		}
		throw invalid("Unterminated string");
	}

	private int readHexCodeUnit() throws IOException {
		@Var int result = 0;
		for (int i = 0; i < 4; ++i) {
			int digit = Character.digit(reader.read(), 16);
			if (digit < 0)
				throw invalid("Invalid Unicode escape");
			result = result * 16 + digit;
		}
		return result;
	}

	private N readNumber(int first) throws IOException {
		StringBuilder text = new StringBuilder().append((char) first);
		@Var int ch = first == '-' ? reader.read() : first;
		if (!isDigit(ch))
			throw invalid("Expected digit");
		if (first == '-')
			text.append((char) ch);
		if (ch == '0') {
			ch = reader.read();
			if (isDigit(ch))
				throw invalid("Leading zero in number");
		} else {
			for (ch = reader.read(); isDigit(ch); ch = reader.read())
				text.append((char) ch);
		}
		if (ch == '.') {
			text.append('.');
			ch = readDigits(text, reader.read());
		}
		if (ch == 'e' || ch == 'E') {
			text.append((char) ch);
			ch = reader.read();
			if (ch == '+' || ch == '-') {
				text.append((char) ch);
				ch = reader.read();
			}
			ch = readDigits(text, ch);
		}
		if (ch >= 0)
			reader.unread(ch);
		// Every parsed number keeps its literal, as a program literal and tonumber do. Spelling an integer
		// as an int node instead would make it indistinguishable from a number this library computed, and
		// the two differ: jq negates a literal zero to +0 but a computed one to -0 from 1.8.0 on.
		return provider.createNumber(new BigDecimal(text.toString()));
	}

	private int readDigits(StringBuilder text, @Var int ch) throws IOException {
		if (!isDigit(ch))
			throw invalid("Expected digit");
		do {
			text.append((char) ch);
			ch = reader.read();
		} while (isDigit(ch));
		return ch;
	}

	private void readLiteral(String suffix) throws IOException {
		for (int i = 0; i < suffix.length(); ++i) {
			if (reader.read() != suffix.charAt(i))
				throw invalid("Invalid literal");
		}
	}

	private int nextNonWhitespace() throws IOException {
		@Var int ch;
		do {
			ch = reader.read();
		} while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r');
		return ch;
	}

	private static boolean isDigit(int ch) {
		return ch >= '0' && ch <= '9';
	}

	private static JsonException invalid(String message) {
		return new JsonException(message);
	}

	@Override
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			throw new JsonException(e);
		}
	}
}
