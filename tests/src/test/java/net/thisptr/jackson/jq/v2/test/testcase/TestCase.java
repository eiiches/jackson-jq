package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCase {
	@JsonProperty("q")
	public String q = "";

	@JsonProperty("in")
	public JsonNode in = NullNode.getInstance();

	@JsonProperty("out")
	public List<JsonNode> out = Collections.emptyList();

	@JsonProperty("file")
	public String file = "";

	@JsonProperty("failing")
	public @Nullable Boolean failing;

	@JsonProperty("should_compile")
	public boolean shouldCompile = true;

	@JsonProperty("ignore_true_jq_behavior")
	public boolean ignoreTrueJqBehavior = false;

	@JsonProperty("numerical_errors")
	public double numericalErrors = 0;

	@JsonProperty("ignore_field_order")
	public boolean ignoreFieldOrder = false;

	/**
	 * jq modules this test case needs on the module search path, keyed by path relative to the
	 * search root (e.g. {@code "a.jq"}, {@code "lib/jq/e/e.jq"}), value is the raw file content.
	 * The test harnesses materialize these files on a module search path before evaluating the test
	 * case.
	 */
	@JsonProperty("modules")
	public Map<String, String> modules = Collections.emptyMap();

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("v")
	@JsonDeserialize(using = VersionRangeDeserializer.class)
	@JsonSerialize(using = ToStringSerializer.class)
	public @Nullable VersionRange version;

	@JsonProperty("comment")
	public @Nullable String comment;

	@Override
	public String toString() {
		return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", q, in, out, version != null ? version : "any");
	}
}
