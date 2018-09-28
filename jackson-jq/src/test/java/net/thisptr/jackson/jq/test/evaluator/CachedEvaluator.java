package net.thisptr.jackson.jq.test.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import net.thisptr.jackson.jq.Version;

/**
 * <p>{@link Evaluator} that caches a {@link Result} persistently across JVM runs. This can save
 * 20~22 seconds on my machine when evaluating approx. 1200 jq expressions.</p>
 *
 * Note that type of {@link Result#error} is not preserved when cached. Don't use <tt>instanceof</tt> or <tt>getClass()</tt> on the field.
 */
public class CachedEvaluator implements AutoCloseable, Evaluator {
	private final DB db;
	private final Evaluator evaluator;

	public CachedEvaluator(final Evaluator evaluator, final String path) {
		this(evaluator, new File(path));
	}

	public CachedEvaluator(final Evaluator evaluator, final File path) {
		this.evaluator = evaluator;
		final Options options = new Options();
		options.createIfMissing(true);
		try {
			this.db = JniDBFactory.factory.open(path, options);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			this.db.close();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class Key {
		@JsonProperty("q")
		private String q;

		@JsonProperty("in")
		private JsonNode in;

		@JsonProperty("v")
		@JsonSerialize(using = ToStringSerializer.class)
		private Version v;

		public Key(final String q, final JsonNode in, final Version v) {
			this.q = q;
			this.in = in;
			this.v = v;
		}
	}

	private static class Value {
		@JsonProperty("out")
		private List<JsonNode> out;

		@JsonProperty("error")
		private String error;
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private void store(final String q, final JsonNode in, final Version v, final Result r) {
		try {
			final byte[] key = MAPPER.writeValueAsBytes(new Key(q, in, v));
			final CachedEvaluator.Value value = new Value();
			value.out = r.values;
			if (r.error != null) {
				value.error = r.error.getMessage();
				if (value.error == null)
					value.error = "null";
			}
			db.put(key, MAPPER.writeValueAsBytes(value));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Result load(final String q, final JsonNode in, final Version v) {
		try {
			final byte[] key = MAPPER.writeValueAsBytes(new Key(q, in, v));
			final byte[] bytes = db.get(key);
			if (bytes == null)
				return null;
			final CachedEvaluator.Value value = MAPPER.readValue(bytes, CachedEvaluator.Value.class);
			return new Result(value.out, value.error != null ? new RuntimeException(value.error) : null);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Result loadOrCompute(final String q, final JsonNode in, final Version v, final Supplier<Result> fn) {
		Result r = load(q, in, v);
		if (r != null)
			return r;
		r = fn.get();
		store(q, in, v, r);
		return r;
	}

	@Override
	public Result evaluate(final String q, final JsonNode in, final Version v, final long timeout) throws Throwable {
		return loadOrCompute(q, in, v, () -> {
			try {
				return evaluator.evaluate(q, in, v, timeout);
			} catch (final Throwable e) {
				throw new RuntimeException(e);
			}
		});
	}
}