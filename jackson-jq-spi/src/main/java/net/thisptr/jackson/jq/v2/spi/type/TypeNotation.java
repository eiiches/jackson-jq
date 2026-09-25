package net.thisptr.jackson.jq.v2.spi.type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.errorprone.annotations.Var;

/**
 * The textual notation of this package, in both directions.
 * <p>
 * Printing and parsing are inverses: {@code parse(print(type))} equals {@code type} for every type
 * this package can build. The reverse does not hold, because the factory methods normalize -- a
 * union of one alternative is that alternative, {@code [*:NEVER]} is the closed empty array, and a
 * quoted field name that is an identifier prints bare. Both directions live here so that neither can
 * drift from the other.
 *
 * <h2>Grammar</h2>
 * <pre>
 * TypeScheme    ::= Quantifier? Signature
 * Quantifier    ::= "&lt;" Binder ("," Binder)* "&gt;"
 * Binder        ::= IDENT (":" Type)?
 * Signature     ::= FunctionType | FilterType
 * FunctionType  ::= "(" (FilterType (";" FilterType)*)? ")" "=&gt;" "(" FilterType ")"
 * FilterType    ::= Type "-&gt;" Type
 *
 * Type          ::= Alternative ("|" Alternative)*
 * Alternative   ::= "ANY" | "NEVER" | "UNDEFINED" | "NULL" | "BOOLEAN" | "STRING" | "BINARY"
 *                 | "NUMBER" | "INT" | "FLOAT"
 *                 | "true" | "false" | STRING
 *                 | ArrayType | ObjectType | RecursiveType
 *                 | IDENT
 *
 * ArrayType     ::= "[" (Type ("," Type)* ("," Rest)? | Rest)? "]"
 * ObjectType    ::= "{" (Field ("," Field)* ("," Rest)? | Rest)? "}"
 * Field         ::= (IDENT | STRING) ":" Type
 * Rest          ::= "*" ":" Type
 * RecursiveType ::= "RECURSIVE" "&lt;" IDENT "=" Type "&gt;"
 * </pre>
 * {@code IDENT} is {@code [A-Za-z_][A-Za-z0-9_]*} and {@code STRING} is a JSON string literal. A
 * {@code STRING} in type position is the type of that one string, and {@code true} and {@code false}
 * are the types of those booleans.
 * Whitespace between tokens is insignificant on input.
 * <p>
 * The grammar is LL(1). Every alternative is chosen by its first token, {@code |} is the only infix
 * operator in a type and unions flatten, so there is no precedence to resolve and no parentheses in
 * type position -- which is what leaves a leading {@code (} to mean a function's parameter list.
 */
final class TypeNotation {
	/**
	 * The words that are always keywords. A type variable may not be named after one, since it would
	 * be read back as the keyword; an object field may, because a field name is only ever read where
	 * a type cannot appear.
	 */
	private static final Set<String> RESERVED_NAMES = Set.of(
			"ANY", "NEVER", "UNDEFINED", "NULL", "BOOLEAN", "STRING", "BINARY",
			"NUMBER", "INT", "FLOAT", "RECURSIVE", "true", "false");

	private TypeNotation() {
	}

	static boolean isReservedName(String name) {
		return RESERVED_NAMES.contains(name);
	}

	/**
	 * Returns whether {@code text} is an {@code IDENT}, which is what may be written unquoted.
	 */
	static boolean isIdentifier(String text) {
		if (text.isEmpty())
			return false;
		for (int i = 0; i < text.length(); i++) {
			if (!isIdentifierChar(text.charAt(i), i == 0))
				return false;
		}
		return true;
	}

	private static boolean isIdentifierChar(char ch, boolean first) {
		if (ch == '_' || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))
			return true;
		return !first && ch >= '0' && ch <= '9';
	}

	// Printing.

	static String print(Type type) {
		StringBuilder result = new StringBuilder();
		appendType(result, type);
		return result.toString();
	}

	static String print(FilterType filter) {
		StringBuilder result = new StringBuilder();
		appendFilter(result, filter);
		return result.toString();
	}

	static String print(FunctionType function) {
		StringBuilder result = new StringBuilder();
		appendFunction(result, function);
		return result.toString();
	}

	static String print(Map<TypeVariable, Type> variables, Object body) {
		StringBuilder result = new StringBuilder();
		if (!variables.isEmpty()) {
			result.append('<');
			@Var boolean first = true;
			for (Map.Entry<TypeVariable, Type> entry : variables.entrySet()) {
				if (!first)
					result.append(", ");
				first = false;
				TypeVariable variable = entry.getKey();
				result.append(variable.name());
				Type upperBound = Objects.requireNonNullElse(entry.getValue(), AnyType.getInstance());
				if (upperBound != AnyType.getInstance()) {
					result.append(": ");
					appendType(result, upperBound);
				}
			}
			result.append("> ");
		}
		if (body instanceof FilterType filter)
			appendFilter(result, filter);
		else if (body instanceof FunctionType function)
			appendFunction(result, function);
		else
			throw new IllegalStateException("Unsupported type scheme body: " + body.getClass().getName());
		return result.toString();
	}

	private static void appendType(StringBuilder out, Type type) {
		// The dataless types are their own spelling, so their toString is the whole definition.
		if (type instanceof AnyType || type instanceof BinaryType || type instanceof NeverType
				|| type instanceof NullType || type instanceof UndefinedType) {
			out.append(type);
		} else if (type instanceof StringType string) {
			String value = string.value();
			if (value == null)
				out.append("STRING");
			else
				appendQuoted(out, value);
		} else if (type instanceof BooleanType bool) {
			Boolean value = bool.value();
			if (value == null)
				out.append("BOOLEAN");
			else
				out.append(value ? "true" : "false");
		} else if (type instanceof NumericType numeric) {
			BigInteger value = numeric.value();
			if (value != null) {
				out.append(value);
			} else {
				out.append(switch (numeric.numberKind()) {
					case UNKNOWN -> "NUMBER";
					case INT -> "INT";
					case FLOAT -> "FLOAT";
				});
			}
		} else if (type instanceof TypeVariable variable) {
			out.append(variable.name());
		} else if (type instanceof UnionType union) {
			@Var
			boolean first = true;
			for (Type alternative : union.alternatives()) {
				if (!first)
					out.append('|');
				first = false;
				appendType(out, alternative);
			}
		} else if (type instanceof ArrayType array) {
			appendArray(out, array);
		} else if (type instanceof ObjectType object) {
			appendObject(out, object);
		} else if (type instanceof RecursiveType recursive) {
			out.append("RECURSIVE<").append(recursive.variable().name()).append(" = ");
			appendType(out, recursive.body());
			out.append('>');
		} else {
			throw new IllegalStateException("Unsupported type: " + type.getClass().getName());
		}
	}

	private static void appendArray(StringBuilder out, ArrayType array) {
		out.append('[');
		@Var
		boolean first = true;
		for (Type knownElement : array.knownElements()) {
			if (!first)
				out.append(',');
			first = false;
			appendType(out, knownElement);
		}
		// A closed array with no known elements is the empty array, which [] already says.
		if (!array.isClosed()) {
			if (!first)
				out.append(',');
			out.append("*:");
			appendType(out, array.additionalElementType());
		}
		out.append(']');
	}

	private static void appendObject(StringBuilder out, ObjectType object) {
		out.append('{');
		@Var
		boolean first = true;
		for (Map.Entry<String, Type> field : object.fields().entrySet()) {
			if (!first)
				out.append(',');
			first = false;
			appendFieldName(out, field.getKey());
			out.append(':');
			appendType(out, field.getValue());
		}
		if (!object.isClosed()) {
			if (!first)
				out.append(',');
			out.append("*:");
			appendType(out, object.additionalFieldType());
		}
		out.append('}');
	}

	private static void appendFilter(StringBuilder out, FilterType filter) {
		appendType(out, filter.inputType());
		out.append(" -> ");
		appendType(out, filter.outputType());
	}

	private static void appendFunction(StringBuilder out, FunctionType function) {
		out.append('(');
		@Var
		boolean first = true;
		for (FilterType parameterType : function.parameterTypes()) {
			if (!first)
				out.append("; ");
			first = false;
			appendFilter(out, parameterType);
		}
		out.append(") => (");
		appendFilter(out, function.returnType());
		out.append(')');
	}

	private static void appendFieldName(StringBuilder out, String name) {
		if (isIdentifier(name)) {
			out.append(name);
			return;
		}
		appendQuoted(out, name);
	}

	/**
	 * Writes a JSON string literal, which is how both a field name that is not an identifier and the
	 * type of a known string are spelled.
	 */
	private static void appendQuoted(StringBuilder out, String value) {
		out.append('"');
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			switch (ch) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\b' -> out.append("\\b");
				case '\f' -> out.append("\\f");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (ch < 0x20)
						out.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
					else
						out.append(ch);
				}
			}
		}
		out.append('"');
	}

	// Parsing.

	static Type parseType(String text) {
		Parser parser = new Parser(text);
		Type type = parser.parseUnion();
		parser.expectEnd();
		return type;
	}

	static FilterType parseFilterType(String text) {
		Parser parser = new Parser(text);
		FilterType filter = parser.parseFilter();
		parser.expectEnd();
		return filter;
	}

	static FunctionType parseFunctionType(String text) {
		Parser parser = new Parser(text);
		FunctionType function = parser.parseFunction();
		parser.expectEnd();
		return function;
	}

	static TypeScheme<FilterType> parseFilterScheme(String text) {
		Parser parser = new Parser(text);
		Map<TypeVariable, Type> quantifier = parser.parseOptionalQuantifier();
		FilterType body = parser.parseFilter();
		parser.expectEnd();
		return TypeScheme.of(quantifier, body);
	}

	static TypeScheme<FunctionType> parseFunctionScheme(String text) {
		Parser parser = new Parser(text);
		Map<TypeVariable, Type> quantifier = parser.parseOptionalQuantifier();
		FunctionType body = parser.parseFunction();
		parser.expectEnd();
		return TypeScheme.of(quantifier, body);
	}

	/**
	 * A recursive-descent parser over the notation. One instance parses one string; the scope stack
	 * holds the variables bound by an enclosing quantifier or {@code RECURSIVE}, innermost last.
	 */
	private static final class Parser {
		private final String text;
		private final List<Map<String, TypeVariable>> scopes = new ArrayList<>();
		private int pos;

		Parser(String text) {
			this.text = text;
		}

		Type parseUnion() {
			List<Type> alternatives = new ArrayList<>();
			alternatives.add(parseAlternative());
			while (peek('|')) {
				pos++;
				alternatives.add(parseAlternative());
			}
			return UnionType.of(alternatives);
		}

		private Type parseAlternative() {
			skipWhitespace();
			if (pos >= text.length())
				throw error("Expected a type");
			if (peek('['))
				return parseArray();
			if (peek('{'))
				return parseObject();
			if (peek('"'))
				return StringType.of(readQuoted());
			if (peekInteger())
				return NumericType.of(readInteger());
			String word = readIdentifier("a type");
			return switch (word) {
				case "ANY" -> AnyType.getInstance();
				case "NEVER" -> NeverType.getInstance();
				case "UNDEFINED" -> UndefinedType.getInstance();
				case "NULL" -> NullType.getInstance();
				case "BOOLEAN" -> BooleanType.getInstance();
				case "STRING" -> StringType.getInstance();
				case "BINARY" -> BinaryType.getInstance();
				case "NUMBER" -> NumericType.getInstance();
				case "INT" -> NumericType.of(NumberKind.INT);
				case "FLOAT" -> NumericType.of(NumberKind.FLOAT);
				case "RECURSIVE" -> parseRecursive();
				case "true" -> BooleanType.of(true);
				case "false" -> BooleanType.of(false);
				default -> resolve(word);
			};
		}

		private Type parseArray() {
			expect('[');
			List<Type> knownElements = new ArrayList<>();
			@Var
			Type additionalElementType = NeverType.getInstance();
			skipWhitespace();
			if (!peek(']')) {
				while (true) {
					skipWhitespace();
					if (peek('*')) {
						pos++;
						expect(':');
						additionalElementType = parseUnion();
						skipWhitespace();
						if (peek(','))
							throw error("Expected ']', since '*:' describes the elements past the known ones");
						break;
					}
					knownElements.add(parseUnion());
					skipWhitespace();
					if (!peek(','))
						break;
					pos++;
				}
			}
			expect(']');
			try {
				return additionalElementType == NeverType.getInstance()
						? ArrayType.of(knownElements)
						: ArrayType.of(knownElements, additionalElementType);
			} catch (IllegalArgumentException e) {
				throw error(Objects.requireNonNullElse(e.getMessage(), "Invalid array type"));
			}
		}

		private Type parseObject() {
			expect('{');
			Map<String, Type> fields = new LinkedHashMap<>();
			@Var
			Type additionalFieldType = NeverType.getInstance();
			skipWhitespace();
			if (!peek('}')) {
				while (true) {
					skipWhitespace();
					if (peek('*')) {
						pos++;
						expect(':');
						additionalFieldType = parseUnion();
						skipWhitespace();
						if (peek(','))
							throw error("Expected '}', since '*:' describes the fields other than the declared ones");
						break;
					}
					String name = parseFieldName();
					expect(':');
					Type type = parseUnion();
					if (fields.put(name, type) != null)
						throw error("Duplicate field: " + name);
					skipWhitespace();
					if (!peek(','))
						break;
					pos++;
				}
			}
			expect('}');
			return additionalFieldType == NeverType.getInstance()
					? ObjectType.of(fields)
					: ObjectType.of(fields, additionalFieldType);
		}

		private String parseFieldName() {
			skipWhitespace();
			if (peek('"'))
				return readQuoted();
			return readIdentifier("a field name");
		}

		private Type parseRecursive() {
			expect('<');
			String name = readVariableName();
			expect('=');
			TypeVariable variable = TypeVariable.of(name);
			Map<String, TypeVariable> scope = new LinkedHashMap<>();
			scope.put(name, variable);
			scopes.add(scope);
			Type body;
			try {
				body = parseUnion();
			} finally {
				scopes.remove(scopes.size() - 1);
			}
			expect('>');
			// Type.recursive rejects a body that never reaches the variable, or reaches it unguarded.
			return RecursiveType.of(variable, body);
		}

		Map<TypeVariable, Type> parseOptionalQuantifier() {
			skipWhitespace();
			if (!peek('<'))
				return Map.of();
			expect('<');
			// The scope is filled as it is parsed, so one binder's bound may name an earlier binder.
			Map<TypeVariable, Type> upperBounds = new LinkedHashMap<>();
			Map<String, TypeVariable> scope = new LinkedHashMap<>();
			scopes.add(scope);
			while (true) {
				String name = readVariableName();
				TypeVariable variable = TypeVariable.of(name);
				if (scope.put(name, variable) != null)
					throw error("Duplicate type variable: " + name);
				skipWhitespace();
				@Var
				Type upperBound = AnyType.getInstance();
				if (peek(':')) {
					pos++;
					upperBound = parseUnion();
				}
				upperBounds.put(variable, upperBound);
				skipWhitespace();
				if (!peek(','))
					break;
				pos++;
			}
			expect('>');
			return Collections.unmodifiableMap(upperBounds);
		}

		FilterType parseFilter() {
			Type inputType = parseUnion();
			expect("->");
			Type outputType = parseUnion();
			return FilterType.of(inputType, outputType);
		}

		FunctionType parseFunction() {
			expect('(');
			List<FilterType> parameterTypes = new ArrayList<>();
			skipWhitespace();
			if (!peek(')')) {
				while (true) {
					parameterTypes.add(parseFilter());
					skipWhitespace();
					if (!peek(';'))
						break;
					pos++;
				}
			}
			expect(')');
			expect("=>");
			expect('(');
			FilterType signature = parseFilter();
			expect(')');
			return FunctionType.of(signature, parameterTypes.toArray(FilterType[]::new));
		}

		private TypeVariable resolve(String name) {
			for (int i = scopes.size() - 1; i >= 0; i--) {
				TypeVariable variable = scopes.get(i).get(name);
				if (variable != null)
					return variable;
			}
			// A variable with no binder in sight is unbounded; only a binder can say otherwise.
			return TypeVariable.of(name);
		}

		private String readVariableName() {
			int start = skipWhitespaceAt();
			String name = readIdentifier("a type variable name");
			if (isReservedName(name))
				throw errorAt(start, "Expected a type variable name, but '" + name + "' is a reserved type name");
			return name;
		}

		private String readIdentifier(String expected) {
			skipWhitespace();
			int start = pos;
			while (pos < text.length() && isIdentifierChar(text.charAt(pos), pos == start))
				pos++;
			if (pos == start)
				throw error("Expected " + expected);
			return text.substring(start, pos);
		}

		private String readQuoted() {
			expect('"');
			StringBuilder result = new StringBuilder();
			while (true) {
				if (pos >= text.length())
					throw error("Unterminated string");
				char ch = text.charAt(pos++);
				if (ch == '"')
					return result.toString();
				if (ch != '\\') {
					result.append(ch);
					continue;
				}
				if (pos >= text.length())
					throw error("Unterminated escape sequence");
				char escape = text.charAt(pos++);
				switch (escape) {
					case '"' -> result.append('"');
					case '\\' -> result.append('\\');
					case '/' -> result.append('/');
					case 'b' -> result.append('\b');
					case 'f' -> result.append('\f');
					case 'n' -> result.append('\n');
					case 'r' -> result.append('\r');
					case 't' -> result.append('\t');
					case 'u' -> result.append(readUnicodeEscape());
					default -> throw error("Unknown escape sequence: \\" + escape);
				}
			}
		}

		private char readUnicodeEscape() {
			if (pos + 4 > text.length())
				throw error("Truncated \\u escape sequence");
			@Var
			int value = 0;
			for (int i = 0; i < 4; i++) {
				int digit = Character.digit(text.charAt(pos + i), 16);
				if (digit < 0)
					throw error("Malformed \\u escape sequence");
				value = value * 16 + digit;
			}
			pos += 4;
			return (char) value;
		}

		private boolean peekInteger() {
			skipWhitespace();
			if (pos >= text.length())
				return false;
			char ch = text.charAt(pos);
			if (ch >= '0' && ch <= '9')
				return true;
			if (ch == '-' && pos + 1 < text.length()) {
				char next = text.charAt(pos + 1);
				return next >= '0' && next <= '9';
			}
			return false;
		}

		private BigInteger readInteger() {
			skipWhitespace();
			int start = pos;
			if (text.charAt(pos) == '-')
				pos++;
			while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9')
				pos++;
			return new BigInteger(text.substring(start, pos));
		}

		void expectEnd() {
			skipWhitespace();
			if (pos < text.length())
				throw error("Unexpected trailing input");
		}

		private boolean peek(char ch) {
			skipWhitespace();
			return pos < text.length() && text.charAt(pos) == ch;
		}

		private void expect(char ch) {
			if (!peek(ch))
				throw error("Expected '" + ch + "'");
			pos++;
		}

		private void expect(String symbol) {
			skipWhitespace();
			if (!text.startsWith(symbol, pos))
				throw error("Expected '" + symbol + "'");
			pos += symbol.length();
		}

		private void skipWhitespace() {
			while (pos < text.length()) {
				char ch = text.charAt(pos);
				if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n')
					break;
				pos++;
			}
		}

		private int skipWhitespaceAt() {
			skipWhitespace();
			return pos;
		}

		private IllegalArgumentException error(String detail) {
			return errorAt(pos, detail);
		}

		private IllegalArgumentException errorAt(int offset, String detail) {
			return new IllegalArgumentException(detail + " at offset " + offset + ": " + text);
		}
	}
}
