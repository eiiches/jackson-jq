package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class JsonWriteFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	/**
	 * {@code indent} is the one option that also accepts null, meaning no indentation.
	 */
	private static final Map<String, Type> OPTION_TYPES = optionTypes();
	private static final Set<String> ALLOWED_OPTIONS = OPTION_TYPES.keySet();
	/**
	 * Indexed by argument count; index 0 is unused because the path is required.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NullType.getInstance(), FilterType.of(INPUT, StringType.getInstance())))),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NullType.getInstance(), FilterType.of(INPUT, StringType.getInstance()),
					FilterType.of(INPUT, ObjectType.of(OPTION_TYPES))))));
	private static final Options DEFAULT_OPTIONS = new Options(null, StandardCharsets.UTF_8, false, true, false);

	private static Map<String, Type> optionTypes() {
		Map<String, Type> types = new HashMap<>(FileFunctionSupport.COMMON_WRITE_OPTIONS);
		types.put("encoding", FileFunctionSupport.OPTIONAL_STRING);
		types.put("newline", FileFunctionSupport.OPTIONAL_BOOLEAN);
		types.put("indent", UnionType.of(NumericType.getInstance(), BooleanType.getInstance(), StringType.getInstance(), NullType.getInstance(), UndefinedType.getInstance()));
		return Map.copyOf(types);
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 2)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		Cardinality first = arguments.get(0).cardinality();
		if (arguments.size() == 1 || first == Cardinality.ZERO)
			return new ExpressionProperties(first, true, true);
		Cardinality second = arguments.get(1).cardinality();
		Cardinality cardinality = second == Cardinality.ZERO ? Cardinality.ZERO
				: first == Cardinality.ONE && second == Cardinality.ONE ? Cardinality.ONE : Cardinality.UNKNOWN;
		return new ExpressionProperties(cardinality, true, true);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> pathExpression = arguments.get(0);
		Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, "fs::write_json");
					if (optionsExpression == null) {
						byte[] bytes = formatAndEncode(jsonProvider, input, DEFAULT_OPTIONS);
						FileFunctionSupport.write(file, bytes, false, false, "fs::write_json");
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						Options options = parseOptions(jsonProvider, optionsNode);
						byte[] bytes = formatAndEncode(jsonProvider, input, options);
						FileFunctionSupport.write(file, bytes, options.append(), options.createParents(), "fs::write_json");
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> Options parseOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, "fs::write_json", ALLOWED_OPTIONS);
		String indent = FileFunctionSupport.parseIndentOption(jsonProvider, optionsNode, "fs::write_json");
		Charset encoding = FileFunctionSupport.parseEncodingOption(jsonProvider, optionsNode, "fs::write_json");
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, "fs::write_json", "append", false);
		boolean newline = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, "fs::write_json", "newline", true);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, "fs::write_json");
		return new Options(indent, encoding, append, newline, createParents);
	}

	private static <JsonNode> byte[] formatAndEncode(JsonProvider<JsonNode> jsonProvider, JsonNode input, Options options) {
		String formatted = JsonPrettyPrinter.print(jsonProvider, input, options.indent());
		String text = options.newline() ? formatted + "\n" : formatted;
		return encode(text, options.encoding());
	}

	private static byte[] encode(String text, Charset charset) {
		try {
			ByteBuffer encoded = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new JsonQueryException("fs::write_json failed to encode the input using " + charset.name() + ": " + e.getMessage(), e);
		}
	}

	private record Options(@Nullable String indent, Charset encoding, boolean append, boolean newline,
						   boolean createParents) {
	}
}
