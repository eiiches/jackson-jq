package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.errorprone.annotations.Var;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * <p>{@link Evaluator} that caches a {@link Result} persistently across JVM runs. This can save
 * 20~22 seconds on my machine when evaluating approx. 1200 jq expressions.</p>
 *
 * Note that type of {@link Result#error} is not preserved when cached. Don't use <tt>instanceof</tt> or <tt>getClass()</tt> on the field.
 */
public class CachedEvaluator implements AutoCloseable, Evaluator {
	private final RocksDB db;
	private final Evaluator evaluator;

	public CachedEvaluator(Evaluator evaluator, String path) {
		this(evaluator, new File(path));
	}

	public CachedEvaluator(Evaluator evaluator, File path) {
		this.evaluator = evaluator;
		Options options = new Options();
		options.setCreateIfMissing(true);
		try {
			this.db = RocksDB.open(options, path.getAbsolutePath());
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			this.db.close();
		} catch (Exception e) {
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

		Key(String q, JsonNode in, Version v) {
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

	private void store(String q, JsonNode in, Version v, Result r) {
		try {
			byte[] key = MAPPER.writeValueAsBytes(new Key(q, in, v));
			CachedEvaluator.Value value = new Value();
			value.out = r.values;
			if (r.error != null) {
				value.error = r.error.getMessage();
				if (value.error == null)
					value.error = "null";
			}
			db.put(key, MAPPER.writeValueAsBytes(value));
		} catch (IOException | RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	private Result load(String q, JsonNode in, Version v) {
		try {
			byte[] key = MAPPER.writeValueAsBytes(new Key(q, in, v));
			byte[] bytes = db.get(key);
			if (bytes == null)
				return null;
			CachedEvaluator.Value value = MAPPER.readValue(bytes, CachedEvaluator.Value.class);
			return new Result(value.out, value.error != null ? new RuntimeException(value.error) : null);
		} catch (IOException | RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	private Result loadOrCompute(String q, JsonNode in, Version v, Supplier<Result> fn) {
		@Var Result r = load(q, in, v);
		if (r != null)
			return r;
		r = fn.get();
		store(q, in, v, r);
		return r;
	}

	@Override
	public Result evaluate(String q, JsonNode in, Version v, long timeout) throws Throwable {
		return loadOrCompute(q, in, v, () -> {
			try {
				return evaluator.evaluate(q, in, v, timeout);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		});
	}
}
