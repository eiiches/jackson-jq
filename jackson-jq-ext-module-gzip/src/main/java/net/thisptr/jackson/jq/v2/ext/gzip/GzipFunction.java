package net.thisptr.jackson.jq.v2.ext.gzip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

final class GzipFunction implements Function {
	private final String name;
	private final boolean compress;
	private final boolean text;

	GzipFunction(String name, boolean compress, boolean text) {
		this.name = name;
		this.compress = compress;
		this.text = text;
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		String function = "gzip::" + name;
		boolean binarySupported = CompressionSupport.supportsBinary(jsonProvider);
		Expression<Context, JsonNode> optionsExpression = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {
			@Override
			public Cardinality getCardinality() {
				return optionsExpression == null ? Cardinality.ONE : optionsExpression.getCardinality();
			}

			@Override
			public boolean dependsOnInput() {
				return true;
			}

			@Override
			public boolean dependsOnExternalState() {
				return optionsExpression != null && optionsExpression.dependsOnExternalState();
			}

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
		try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
			gzip.write(input);
		}
	}

	private static InputStream decompress(byte[] input) throws IOException {
		return new GZIPInputStream(new ByteArrayInputStream(input));
	}
}
