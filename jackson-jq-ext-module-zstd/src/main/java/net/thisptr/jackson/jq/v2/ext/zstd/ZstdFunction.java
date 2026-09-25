package net.thisptr.jackson.jq.v2.ext.zstd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class ZstdFunction implements Function {
	private final String name;
	private final boolean compress;
	private final boolean text;
	private final List<List<TypeScheme<FunctionType>>> typeSchemes;

	ZstdFunction(String name, boolean compress, boolean text) {
		this.name = name;
		this.compress = compress;
		this.text = text;
		this.typeSchemes = typeSchemes(compress, text);
	}

	/**
	 * Indexed by argument count. Only the text conversions take the charset option, so the binary ones
	 * are registered at arity 0 alone and declare nothing for arity 1.
	 */
	private static List<List<TypeScheme<FunctionType>>> typeSchemes(boolean compress, boolean text) {
		Type inputType = compress && text ? StringType.getInstance() : BinaryType.getInstance();
		Type outputType = !compress && text ? StringType.getInstance() : BinaryType.getInstance();
		List<TypeScheme<FunctionType>> withoutOptions = List.of(
				TypeScheme.of(FunctionType.of(inputType, outputType)));
		if (!text)
			return List.of(withoutOptions);
		return List.of(withoutOptions, List.of(TypeScheme.of(FunctionType.of(inputType, outputType, FilterType.of(inputType, CompressionSupport.OPTIONS)))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 0 || totalArguments >= typeSchemes.size())
			return List.of();
		return typeSchemes.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		Cardinality cardinality = arguments.isEmpty() ? Cardinality.ONE : arguments.get(0).cardinality();
		boolean external = !arguments.isEmpty() && arguments.get(0).dependsOnExternalState();
		return new ExpressionProperties(cardinality, true, external);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		String function = "zstd::" + name;
		boolean binarySupported = CompressionSupport.supportsBinary(jsonProvider);
		Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				if (optionsExpression == null) {
					output.emit(evaluate(context.getRuntimeLimits(), input, StandardCharsets.UTF_8), UntrackedPath.getInstance());
					return;
				}
				optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
					Charset charset = CompressionSupport.parseCharset(jsonProvider, function, optionsNode);
					output.emit(evaluate(context.getRuntimeLimits(), input, charset), UntrackedPath.getInstance());
				});
			}

			private JsonNode evaluate(RuntimeLimits limits, JsonNode input, Charset charset) {
				if (compress) {
					byte[] inputBytes = text
							? CompressionSupport.encodeText(function, CompressionSupport.getInputText(jsonProvider, function, input), charset)
							: CompressionSupport.getInputBytes(jsonProvider, function, input);
					return CompressionSupport.createBinaryValue(jsonProvider, compressed(limits, inputBytes), binarySupported);
				}
				byte[] inputBytes = CompressionSupport.getInputBytes(jsonProvider, function, input);
				if (text)
					return jsonProvider.createString(decompressedText(limits, inputBytes, charset));
				return CompressionSupport.createBinaryValue(jsonProvider, decompressed(limits, inputBytes), binarySupported);
			}

			private byte[] compressed(RuntimeLimits limits, byte[] inputBytes) {
				CompressionSupport.LimitedByteArrayOutputStream bytes = limitedOutput(limits);
				try {
					compress(inputBytes, bytes);
				} catch (IOException | IllegalArgumentException e) {
					throw failure(e);
				}
				return bytes.toByteArray();
			}

			private byte[] decompressed(RuntimeLimits limits, byte[] inputBytes) {
				CompressionSupport.LimitedByteArrayOutputStream bytes = limitedOutput(limits);
				try (InputStream stream = decompress(inputBytes)) {
					CompressionSupport.copy(stream, bytes);
				} catch (IOException | IllegalArgumentException e) {
					throw failure(e);
				}
				return bytes.toByteArray();
			}

			private String decompressedText(RuntimeLimits limits, byte[] inputBytes, Charset charset) {
				InputStream stream;
				try {
					stream = decompress(inputBytes);
				} catch (IOException | IllegalArgumentException e) {
					throw failure(e);
				}
				return CompressionSupport.readText(function, stream, charset, limits);
			}

			private CompressionSupport.LimitedByteArrayOutputStream limitedOutput(RuntimeLimits limits) {
				int maximumBytes = binarySupported ? limits.getMaxBinaryLength() : CompressionSupport.maximumBytesForBase64(limits.getMaxStringLength());
				return new CompressionSupport.LimitedByteArrayOutputStream(maximumBytes, binarySupported, limits.getMaxStringLength());
			}

			private JsonQueryException failure(Exception e) {
				return new JsonQueryException(function + " failed: " + e.getMessage(), e);
			}
		};
	}

	private static void compress(byte[] input, OutputStream output) throws IOException {
		try (ZstdOutputStream zstd = new ZstdOutputStream(output)) {
			zstd.write(input);
		}
	}

	private static InputStream decompress(byte[] input) throws IOException {
		return new ZstdInputStream(new ByteArrayInputStream(input));
	}
}
