package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
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
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class FileWriteFunction implements Function {
	/**
	 * Writing bytes takes no charset; writing text adds one.
	 */
	private static final Map<String, Type> BINARY_OPTION_TYPES = FileFunctionSupport.COMMON_WRITE_OPTIONS;
	private static final Map<String, Type> TEXT_OPTION_TYPES = textOptionTypes();
	private static final Set<String> BINARY_ALLOWED_OPTIONS = BINARY_OPTION_TYPES.keySet();
	private static final Set<String> TEXT_ALLOWED_OPTIONS = TEXT_OPTION_TYPES.keySet();

	private final boolean binary;
	/**
	 * Indexed by argument count; index 0 is unused because the path is required.
	 */
	private final List<List<TypeScheme<FunctionType>>> typeSchemes;

	private FileWriteFunction(boolean binary) {
		this.binary = binary;
		Type inputType = binary ? BinaryType.getInstance() : StringType.getInstance();
		Type optionsType = ObjectType.of(binary ? BINARY_OPTION_TYPES : TEXT_OPTION_TYPES);
		this.typeSchemes = List.of(List.of(),
				List.of(TypeScheme.of(FunctionType.of(inputType, NullType.getInstance(), FilterType.of(inputType, StringType.getInstance())))),
				List.of(TypeScheme.of(FunctionType.of(inputType, NullType.getInstance(), FilterType.of(inputType, StringType.getInstance()), FilterType.of(inputType, optionsType)))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 2)
			return List.of();
		return typeSchemes.get(totalArguments);
	}

	private static Map<String, Type> textOptionTypes() {
		Map<String, Type> merged = new HashMap<>(FileFunctionSupport.COMMON_WRITE_OPTIONS);
		merged.put("encoding", FileFunctionSupport.OPTIONAL_STRING);
		return Map.copyOf(merged);
	}

	public static FileWriteFunction text() {
		return new FileWriteFunction(false);
	}

	public static FileWriteFunction binary() {
		return new FileWriteFunction(true);
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
		String function = binary ? "fs::write_binary" : "fs::write_text";
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				if (binary) {
					byte[] bytes = getBinaryInput(jsonProvider, input, function);
					pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
						java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, function);
						if (optionsExpression == null) {
							FileFunctionSupport.write(file, bytes, false, false, function);
							output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
							return;
						}
						optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
							BinaryOptions options = parseBinaryOptions(jsonProvider, optionsNode, function);
							FileFunctionSupport.write(file, bytes, options.append(), options.createParents(), function);
							output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						});
					});
					return;
				}

				String text = getTextInput(jsonProvider, input, function);
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, function);
					if (optionsExpression == null) {
						FileFunctionSupport.write(file, encode(text, StandardCharsets.UTF_8, function), false, false, function);
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						TextOptions options = parseTextOptions(jsonProvider, optionsNode, function);
						byte[] bytes = encode(text, options.charset(), function);
						FileFunctionSupport.write(file, bytes, options.append(), options.createParents(), function);
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> TextOptions parseTextOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode, String function) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, function, TEXT_ALLOWED_OPTIONS);
		Charset charset = FileFunctionSupport.parseEncodingOption(jsonProvider, optionsNode, function);
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, function, "append", false);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, function);
		return new TextOptions(charset, append, createParents);
	}

	private static <JsonNode> BinaryOptions parseBinaryOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode, String function) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, function, BINARY_ALLOWED_OPTIONS);
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, function, "append", false);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, function);
		return new BinaryOptions(append, createParents);
	}

	private static <JsonNode> String getTextInput(JsonProvider<JsonNode> jsonProvider, JsonNode input, String function) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires string input, but got " + type);
		return jsonProvider.getString(input);
	}

	private static <JsonNode> byte[] getBinaryInput(JsonProvider<JsonNode> jsonProvider, JsonNode input, String function) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type == JsonNodeType.BINARY)
			return jsonProvider.getBinaryAsByteArray(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires binary or Base64 string input, but got " + type);
		try {
			return Base64.getDecoder().decode(jsonProvider.getString(input));
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " input must be valid Base64", e);
		}
	}

	private static byte[] encode(String text, Charset charset, String function) {
		try {
			ByteBuffer encoded = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new JsonQueryException(function + " failed to encode the input using " + charset.name() + ": " + e.getMessage(), e);
		}
	}

	private record TextOptions(Charset charset, boolean append, boolean createParents) {
	}

	private record BinaryOptions(boolean append, boolean createParents) {
	}
}
